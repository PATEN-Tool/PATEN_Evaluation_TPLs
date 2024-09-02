// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.web;

import java.util.Hashtable;
import javax.jms.Message;
import javax.jms.Destination;
import javax.jms.Session;
import java.io.IOException;
import java.io.PrintWriter;
import javax.jms.JMSException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.util.Map;

public class PortfolioPublishServlet extends MessageServletSupport
{
    private static final int maxDeltaPercent = 1;
    private static final Map lastPrices;
    private boolean ricoStyle;
    
    public PortfolioPublishServlet() {
        this.ricoStyle = true;
    }
    
    public void init() throws ServletException {
        super.init();
        this.ricoStyle = MessageServletSupport.asBoolean(this.getServletConfig().getInitParameter("rico"), true);
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final PrintWriter out = response.getWriter();
        final String[] stocks = request.getParameterValues("stocks");
        if (stocks == null || stocks.length == 0) {
            out.println("<html><body>No <b>stocks</b> query parameter specified. Cannot publish market data</body></html>");
        }
        else {
            Integer total = (Integer)request.getSession(true).getAttribute("total");
            if (total == null) {
                total = new Integer(0);
            }
            final int count = this.getNumberOfMessages(request);
            total = new Integer(total + count);
            request.getSession().setAttribute("total", (Object)total);
            try {
                final WebClient client = WebClient.getWebClient(request);
                for (int i = 0; i < count; ++i) {
                    this.sendMessage(client, stocks);
                }
                out.print("<html><head><meta http-equiv='refresh' content='");
                String refreshRate = request.getParameter("refresh");
                if (refreshRate == null || refreshRate.length() == 0) {
                    refreshRate = "1";
                }
                out.print(refreshRate);
                out.println("'/></head>");
                out.println("<body>Published <b>" + count + "</b> of " + total + " price messages.  Refresh = " + refreshRate + "s");
                out.println("</body></html>");
            }
            catch (JMSException e) {
                out.println("<html><body>Failed sending price messages due to <b>" + e + "</b></body></html>");
                this.log("Failed to send message: " + e, (Throwable)e);
            }
        }
    }
    
    protected void sendMessage(final WebClient client, final String[] stocks) throws JMSException {
        final Session session = client.getSession();
        int idx = 0;
        do {
            idx = (int)Math.round(stocks.length * Math.random());
        } while (idx >= stocks.length);
        final String stock = stocks[idx];
        final Destination destination = (Destination)session.createTopic("STOCKS." + stock);
        final String stockText = this.createStockText(stock);
        this.log("Sending: " + stockText + " on destination: " + destination);
        final Message message = (Message)session.createTextMessage(stockText);
        client.send(destination, message);
    }
    
    protected String createStockText(final String stock) {
        Double value = PortfolioPublishServlet.lastPrices.get(stock);
        if (value == null) {
            value = new Double(Math.random() * 100.0);
        }
        final double oldPrice = value;
        value = new Double(this.mutatePrice(oldPrice));
        PortfolioPublishServlet.lastPrices.put(stock, value);
        final double price = value;
        final double offer = price * 1.001;
        final String movement = (price > oldPrice) ? "up" : "down";
        return "<price stock='" + stock + "' bid='" + price + "' offer='" + offer + "' movement='" + movement + "'/>";
    }
    
    protected double mutatePrice(final double price) {
        final double percentChange = 2.0 * Math.random() * 1.0 - 1.0;
        return price * (100.0 + percentChange) / 100.0;
    }
    
    protected int getNumberOfMessages(final HttpServletRequest request) {
        final String name = request.getParameter("count");
        if (name != null) {
            return Integer.parseInt(name);
        }
        return 1;
    }
    
    static {
        lastPrices = new Hashtable();
    }
}
