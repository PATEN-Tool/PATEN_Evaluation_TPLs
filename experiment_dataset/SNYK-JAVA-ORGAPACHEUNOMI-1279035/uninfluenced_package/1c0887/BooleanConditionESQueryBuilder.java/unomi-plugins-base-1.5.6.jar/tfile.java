// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.plugins.baseplugin.conditions;

import org.slf4j.LoggerFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import java.util.List;
import org.elasticsearch.index.query.QueryBuilder;
import org.apache.unomi.persistence.elasticsearch.conditions.ConditionESQueryBuilderDispatcher;
import java.util.Map;
import org.apache.unomi.api.conditions.Condition;
import org.slf4j.Logger;
import org.apache.unomi.persistence.elasticsearch.conditions.ConditionESQueryBuilder;

public class BooleanConditionESQueryBuilder implements ConditionESQueryBuilder
{
    private static final Logger logger;
    
    public QueryBuilder buildQuery(final Condition condition, final Map<String, Object> context, final ConditionESQueryBuilderDispatcher dispatcher) {
        final boolean isAndOperator = "and".equalsIgnoreCase((String)condition.getParameter("operator"));
        final List<Condition> conditions = (List<Condition>)condition.getParameter("subConditions");
        final int conditionCount = conditions.size();
        if (conditionCount == 1) {
            return dispatcher.buildFilter((Condition)conditions.get(0), (Map)context);
        }
        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (int i = 0; i < conditionCount; ++i) {
            if (isAndOperator) {
                final QueryBuilder andFilter = dispatcher.buildFilter((Condition)conditions.get(i), (Map)context);
                if (andFilter != null) {
                    if (andFilter.getName().equals("range")) {
                        boolQueryBuilder.filter(andFilter);
                    }
                    else {
                        boolQueryBuilder.must(andFilter);
                    }
                }
                else {
                    BooleanConditionESQueryBuilder.logger.warn("Null filter for boolean AND sub condition. See debug log level for more information");
                    if (BooleanConditionESQueryBuilder.logger.isDebugEnabled()) {
                        BooleanConditionESQueryBuilder.logger.debug("Null filter for boolean AND sub condition {}", (Object)conditions.get(i));
                    }
                }
            }
            else {
                final QueryBuilder orFilter = dispatcher.buildFilter((Condition)conditions.get(i), (Map)context);
                if (orFilter != null) {
                    boolQueryBuilder.should(orFilter);
                }
                else {
                    BooleanConditionESQueryBuilder.logger.warn("Null filter for boolean OR sub condition. See debug log level for more information");
                    if (BooleanConditionESQueryBuilder.logger.isDebugEnabled()) {
                        BooleanConditionESQueryBuilder.logger.debug("Null filter for boolean OR sub condition {}", (Object)conditions.get(i));
                    }
                }
            }
        }
        return (QueryBuilder)boolQueryBuilder;
    }
    
    static {
        logger = LoggerFactory.getLogger(BooleanConditionESQueryBuilder.class.getName());
    }
}
