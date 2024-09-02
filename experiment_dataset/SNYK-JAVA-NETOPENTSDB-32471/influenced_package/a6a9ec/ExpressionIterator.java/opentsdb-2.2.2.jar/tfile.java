// 
// Decompiled by Procyon v0.5.36
// 

package net.opentsdb.query.expression;

import org.slf4j.LoggerFactory;
import net.opentsdb.utils.ByteSet;
import java.util.Collection;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Iterator;
import net.opentsdb.core.FillPolicy;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.commons.jexl2.MapContext;
import java.util.Set;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.Script;
import java.util.Map;
import org.apache.commons.jexl2.JexlEngine;
import org.slf4j.Logger;

public class ExpressionIterator implements ITimeSyncedIterator
{
    private static final Logger LOG;
    public static final JexlEngine JEXL_ENGINE;
    private final boolean intersect_on_query_tagks;
    private final boolean include_agg_tags;
    private final Map<String, ITimeSyncedIterator> results;
    private final Script expression;
    private final JexlContext context;
    private final Set<String> names;
    private VariableIterator iterator;
    private Map<String, ExpressionDataPoint[]> iteration_results;
    private ExpressionDataPoint[] dps;
    private final String id;
    private int index;
    private NumericFillPolicy fill_policy;
    private VariableIterator.SetOperator set_operator;
    
    public ExpressionIterator(final String id, final String expression, final VariableIterator.SetOperator set_operator, final boolean intersect_on_query_tagks, final boolean include_agg_tags) {
        this.context = (JexlContext)new MapContext();
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("The expression cannot be  null");
        }
        if (set_operator == null) {
            throw new IllegalArgumentException("The set operator cannot be null");
        }
        this.id = id;
        this.intersect_on_query_tagks = intersect_on_query_tagks;
        this.include_agg_tags = include_agg_tags;
        this.results = new HashMap<String, ITimeSyncedIterator>();
        this.expression = ExpressionIterator.JEXL_ENGINE.createScript(expression);
        this.names = new HashSet<String>();
        this.extractVariableNames();
        if (this.names.size() < 1) {
            throw new IllegalArgumentException("The expression didn't appear to have any variables");
        }
        this.set_operator = set_operator;
        this.fill_policy = new NumericFillPolicy(FillPolicy.NOT_A_NUMBER);
    }
    
    private ExpressionIterator(final ExpressionIterator iterator) {
        this.context = (JexlContext)new MapContext();
        this.id = iterator.id;
        this.expression = ExpressionIterator.JEXL_ENGINE.createScript(iterator.expression.toString());
        this.intersect_on_query_tagks = iterator.intersect_on_query_tagks;
        this.include_agg_tags = iterator.include_agg_tags;
        this.set_operator = iterator.set_operator;
        this.results = new HashMap<String, ITimeSyncedIterator>();
        for (final Map.Entry<String, ITimeSyncedIterator> entry : iterator.results.entrySet()) {
            this.results.put(entry.getKey(), entry.getValue().getCopy());
        }
        this.names = new HashSet<String>();
        this.extractVariableNames();
        if (this.names.size() < 1) {
            throw new IllegalArgumentException("The expression didn't appear to have any variables");
        }
    }
    
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("ExpressionIterator(id=").append(this.id).append(", expression=\"").append(this.expression.toString()).append(", setOperator=").append(this.set_operator).append(", fillPolicy=").append(this.fill_policy).append(", intersectOnQueryTagks=").append(this.intersect_on_query_tagks).append(", includeAggTags=").append(this.include_agg_tags).append(", index=").append(this.index).append("\", VariableIterator=").append(this.iterator).append(", dps=").append(this.dps).append(", results=").append(this.results).append(")");
        return buf.toString();
    }
    
    public void addResults(final String id, final ITimeSyncedIterator iterator) {
        if (id == null) {
            throw new IllegalArgumentException("Missing ID");
        }
        if (iterator == null) {
            throw new IllegalArgumentException("Iterator cannot be null");
        }
        this.results.put(id, iterator);
    }
    
    public void compile() {
        if (ExpressionIterator.LOG.isDebugEnabled()) {
            ExpressionIterator.LOG.debug("Compiling " + this);
        }
        if (this.results.size() < 1) {
            throw new IllegalArgumentException("No results for any variables in the expression: " + this);
        }
        if (this.results.size() < this.names.size()) {
            throw new IllegalArgumentException("Not enough query results [" + this.results.size() + " total results found] for the expression variables [" + this.names.size() + " expected] " + this);
        }
        for (final String variable : this.names) {
            final ITimeSyncedIterator it = this.results.get(variable.toLowerCase());
            if (it == null) {
                throw new IllegalArgumentException("Missing results for variable " + variable);
            }
            if (it instanceof ExpressionIterator) {
                ((ExpressionIterator)it).compile();
            }
            if (!ExpressionIterator.LOG.isDebugEnabled()) {
                continue;
            }
            ExpressionIterator.LOG.debug("Matched variable " + variable + " to " + it);
        }
        switch (this.set_operator) {
            case INTERSECTION: {
                this.iterator = new IntersectionIterator(this.id, this.results, this.intersect_on_query_tagks, this.include_agg_tags);
                break;
            }
            case UNION: {
                this.iterator = new UnionIterator(this.id, this.results, this.intersect_on_query_tagks, this.include_agg_tags);
                break;
            }
        }
        this.iteration_results = this.iterator.getResults();
        this.dps = new ExpressionDataPoint[this.iterator.getSeriesSize()];
        for (int i = 0; i < this.iterator.getSeriesSize(); ++i) {
            final Iterator<Map.Entry<String, ExpressionDataPoint[]>> it2 = this.iteration_results.entrySet().iterator();
            Map.Entry<String, ExpressionDataPoint[]> entry = it2.next();
            if (entry.getValue() == null || entry.getValue()[i] == null) {
                this.dps[i] = new ExpressionDataPoint();
            }
            else {
                this.dps[i] = new ExpressionDataPoint(entry.getValue()[i]);
            }
            while (it2.hasNext()) {
                entry = it2.next();
                if (entry.getValue() != null && entry.getValue()[i] != null) {
                    this.dps[i].add(entry.getValue()[i]);
                }
            }
        }
        if (ExpressionIterator.LOG.isDebugEnabled()) {
            ExpressionIterator.LOG.debug("Finished compiling " + this);
        }
    }
    
    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }
    
    @Override
    public ExpressionDataPoint[] next(final long timestamp) {
        this.iterator.next();
        for (int i = 0; i < this.iterator.getSeriesSize(); ++i) {
            for (final String variable : this.names) {
                if (this.iteration_results.get(variable)[i] == null) {
                    this.context.set(variable, (Object)this.results.get(variable).getFillPolicy().getValue());
                }
                else {
                    final double val = this.iteration_results.get(variable)[i].toDouble();
                    if (Double.isNaN(val)) {
                        this.context.set(variable, (Object)this.results.get(variable).getFillPolicy().getValue());
                    }
                    else {
                        this.context.set(variable, (Object)val);
                    }
                }
            }
            final Object output = this.expression.execute(this.context);
            double result;
            if (output instanceof Double) {
                result = (double)this.expression.execute(this.context);
            }
            else {
                if (!(output instanceof Boolean)) {
                    throw new IllegalStateException("Expression returned a result of type: " + output.getClass().getName() + " for " + this);
                }
                result = (((boolean)this.expression.execute(this.context)) ? 1 : 0);
            }
            this.dps[i].reset(timestamp, result);
        }
        return this.dps;
    }
    
    @Override
    public ExpressionDataPoint[] values() {
        return this.dps;
    }
    
    private void extractVariableNames() {
        if (this.expression == null) {
            throw new IllegalArgumentException("The expression was null");
        }
        for (final List<String> exp_list : ExpressionIterator.JEXL_ENGINE.getVariables(this.expression)) {
            for (final String variable : exp_list) {
                this.names.add(variable);
            }
        }
    }
    
    public Set<String> getVariableNames() {
        return (Set<String>)ImmutableSet.copyOf((Collection)this.names);
    }
    
    public void setSetOperator(final VariableIterator.SetOperator set_operator) {
        this.set_operator = set_operator;
    }
    
    @Override
    public long nextTimestamp() {
        return this.iterator.nextTimestamp();
    }
    
    @Override
    public int size() {
        return this.dps.length;
    }
    
    @Override
    public void nullIterator(final int index) {
        if (index < 0 || index >= this.dps.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }
    
    @Override
    public int getIndex() {
        return this.index;
    }
    
    @Override
    public void setIndex(final int index) {
        this.index = index;
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public ByteSet getQueryTagKs() {
        return null;
    }
    
    @Override
    public void setFillPolicy(final NumericFillPolicy policy) {
        this.fill_policy = policy;
    }
    
    @Override
    public NumericFillPolicy getFillPolicy() {
        return this.fill_policy;
    }
    
    @Override
    public ITimeSyncedIterator getCopy() {
        final ExpressionIterator ei = new ExpressionIterator(this);
        return ei;
    }
    
    @Override
    public boolean hasNext(final int i) {
        return this.iterator.hasNext(i);
    }
    
    @Override
    public void next(final int i) {
        this.iterator.next(i);
        long ts = Long.MAX_VALUE;
        for (final String variable : this.names) {
            if (this.iteration_results.get(variable)[i] == null) {
                this.context.set(variable, (Object)this.results.get(variable).getFillPolicy().getValue());
            }
            else {
                if (this.iteration_results.get(variable)[i].timestamp() < ts) {
                    ts = this.iteration_results.get(variable)[i].timestamp();
                }
                final double val = this.iteration_results.get(variable)[i].toDouble();
                if (Double.isNaN(val)) {
                    this.context.set(variable, (Object)this.results.get(variable).getFillPolicy().getValue());
                }
                else {
                    this.context.set(variable, (Object)val);
                }
            }
        }
        final double result = (double)this.expression.execute(this.context);
        this.dps[i].reset(ts, result);
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ExpressionIterator.class);
        JEXL_ENGINE = new JexlEngine();
    }
}
