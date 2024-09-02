// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.restrictor.policy;

import java.util.Iterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import org.w3c.dom.Document;
import java.util.regex.Pattern;
import java.util.List;

public class CorsChecker extends AbstractChecker<String>
{
    private boolean strictChecking;
    private List<Pattern> patterns;
    
    public CorsChecker(final Document pDoc) {
        this.strictChecking = false;
        final NodeList corsNodes = pDoc.getElementsByTagName("cors");
        if (corsNodes.getLength() > 0) {
            this.patterns = new ArrayList<Pattern>();
            for (int i = 0; i < corsNodes.getLength(); ++i) {
                final Node corsNode = corsNodes.item(i);
                final NodeList nodes = corsNode.getChildNodes();
                for (int j = 0; j < nodes.getLength(); ++j) {
                    final Node node = nodes.item(j);
                    if (node.getNodeType() == 1) {
                        this.assertNodeName(node, "allow-origin", "strict-checking");
                        if (node.getNodeName().equals("allow-origin")) {
                            String p = node.getTextContent().trim().toLowerCase();
                            p = Pattern.quote(p).replace("*", "\\E.*\\Q");
                            this.patterns.add(Pattern.compile("^" + p + "$"));
                        }
                        else if (node.getNodeName().equals("strict-checking")) {
                            this.strictChecking = true;
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public boolean check(final String pArg) {
        return this.check(pArg, false);
    }
    
    public boolean check(final String pOrigin, final boolean pIsStrictCheck) {
        if (pIsStrictCheck && !this.strictChecking) {
            return true;
        }
        if (this.patterns == null || this.patterns.size() == 0) {
            return true;
        }
        for (final Pattern pattern : this.patterns) {
            if (pattern.matcher(pOrigin).matches()) {
                return true;
            }
        }
        return false;
    }
}
