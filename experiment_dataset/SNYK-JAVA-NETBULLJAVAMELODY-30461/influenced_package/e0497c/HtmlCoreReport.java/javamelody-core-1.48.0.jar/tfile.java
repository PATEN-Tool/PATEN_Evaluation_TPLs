// 
// Decompiled by Procyon v0.5.36
// 

package net.bull.javamelody;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

class HtmlCoreReport extends HtmlAbstractReport
{
    private static final int MAX_CURRENT_REQUESTS_DISPLAYED_IN_MAIN_REPORT = 500;
    private static final int MAX_THREADS_DISPLAYED_IN_MAIN_REPORT = 500;
    private static final String SEPARATOR = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final String END_DIV = "</div>";
    private static final String SCRIPT_BEGIN = "<script type='text/javascript'>";
    private static final String SCRIPT_END = "</script>";
    private final Collector collector;
    private final List<JavaInformations> javaInformationsList;
    private final Range range;
    private final CollectorServer collectorServer;
    private final long start;
    
    HtmlCoreReport(final Collector collector, final CollectorServer collectorServer, final List<JavaInformations> javaInformationsList, final Range range, final Writer writer) {
        super(writer);
        this.start = System.currentTimeMillis();
        assert collector != null;
        assert javaInformationsList != null && !javaInformationsList.isEmpty();
        assert range != null;
        this.collector = collector;
        this.collectorServer = collectorServer;
        this.javaInformationsList = javaInformationsList;
        this.range = range;
    }
    
    @Override
    void toHtml() throws IOException {
        this.toHtml(null, null);
    }
    
    void toHtml(final String message, final String anchorNameForRedirect) throws IOException {
        if (this.collectorServer != null) {
            this.writeApplicationsLinks();
        }
        this.writeln("<h3><a name='top'></a><img width='24' height='24' src='?resource=systemmonitor.png' alt='#Stats#'/>");
        this.writeSummary();
        this.writeln("</h3>");
        this.write("<a href='http://code.google.com/p/javamelody/wiki/Donate'>");
        this.writeln("<img class='noPrint' style='position: absolute; top: 15px; right: 10px; border: 0;' src='?resource=donate.gif' alt='Donate' /></a>");
        this.writeln("<div align='center'>");
        this.writeRefreshAndPeriodLinks(null, null);
        this.writeGraphs();
        this.writeln("</div>");
        final List<Counter> counters = this.collector.getRangeCountersToBeDisplayed(this.range);
        final Map<String, HtmlCounterReport> counterReportsByCounterName = this.writeCounters(counters);
        if (this.range.getPeriod() == Period.TOUT && counterReportsByCounterName.size() > 1) {
            this.writeln("<div align='right'>");
            this.writeln("<a href='?action=clear_counter&amp;counter=all' title='#Vider_toutes_stats#'");
            this.writeln("class='noPrint' onclick=\"javascript:return confirm('" + HtmlAbstractReport.getStringForJavascript("confirm_vider_toutes_stats") + "');\">#Reinitialiser_toutes_stats#</a>");
            this.writeln("</div>");
        }
        if (this.collectorServer == null) {
            this.write("<h3><a name='currentRequests'></a>");
            this.writeln("<img width='24' height='24' src='?resource=hourglass.png' alt='#Requetes_en_cours#'/>#Requetes_en_cours#</h3>");
            this.writeCurrentRequests(this.javaInformationsList.get(0), counters, counterReportsByCounterName);
        }
        this.writeln("<h3><a name='systeminfo'></a><img width='24' height='24' src='?resource=systeminfo.png' alt='#Informations_systemes#'/>");
        this.writeln("#Informations_systemes#</h3>");
        if (this.collectorServer != null) {
            this.writeln("<div align='center' class='noPrint'><a href='?part=currentRequests'>");
            this.writeln("<img src='?resource=hourglass.png' width='20' height='20' alt=\"#Voir_requetes_en_cours#\" /> #Voir_requetes_en_cours#</a>");
            this.writeln("</div>");
            this.writeln("<br/>");
        }
        if (Parameters.isSystemActionsEnabled()) {
            this.writeSystemActionsLinks();
        }
        new HtmlJavaInformationsReport(this.javaInformationsList, this.getWriter()).toHtml();
        this.write("<h3 style='clear:both;'><a name='threads'></a>");
        this.writeln("<img width='24' height='24' src='?resource=threads.png' alt='#Threads#'/>");
        this.writeln("#Threads#</h3>");
        this.writeThreads();
        if (this.isJobEnabled()) {
            this.writeln("<h3><a name='jobs'></a><img width='24' height='24' src='?resource=jobs.png' alt='#Jobs#'/>");
            this.writeln("#Jobs#</h3>");
            final Counter rangeJobCounter = this.collector.getRangeCounter(this.range, "job");
            this.writeJobs(rangeJobCounter);
            this.writeCounter(rangeJobCounter);
        }
        if (this.isCacheEnabled()) {
            this.writeln("<h3><a name='caches'></a><img width='24' height='24' src='?resource=caches.png' alt='#Caches#'/>");
            this.writeln("#Caches#</h3>");
            this.writeCaches();
        }
        this.writeMessageIfNotNull(message, null, anchorNameForRedirect);
        this.writePoweredBy();
        this.writeDurationAndOverhead();
    }
    
    private void writeSummary() throws IOException {
        final String javaMelodyUrl = "<a href='http://javamelody.googlecode.com' target='_blank'>JavaMelody</a>";
        if (this.range.getPeriod() == Period.TOUT) {
            final String startDate = I18N.createDateAndTimeFormat().format(this.collector.getCounters().get(0).getStartDate());
            this.writeDirectly(HtmlAbstractReport.getFormattedString("Statistiques", "<a href='http://javamelody.googlecode.com' target='_blank'>JavaMelody</a>", I18N.getCurrentDateAndTime(), startDate, this.collector.getApplication()));
        }
        else {
            this.writeDirectly(HtmlAbstractReport.getFormattedString("Statistiques_sans_depuis", "<a href='http://javamelody.googlecode.com' target='_blank'>JavaMelody</a>", I18N.getCurrentDateAndTime(), this.collector.getApplication()));
        }
        if (this.javaInformationsList.get(0).getContextDisplayName() != null) {
            this.writeDirectly(HtmlAbstractReport.htmlEncodeButNotSpace(" (" + this.javaInformationsList.get(0).getContextDisplayName() + ')'));
        }
        this.writeln("");
    }
    
    private Map<String, HtmlCounterReport> writeCounters(final List<Counter> counters) throws IOException {
        final Map<String, HtmlCounterReport> counterReportsByCounterName = new HashMap<String, HtmlCounterReport>();
        for (final Counter counter : counters) {
            final HtmlCounterReport htmlCounterReport = this.writeCounter(counter);
            counterReportsByCounterName.put(counter.getName(), htmlCounterReport);
        }
        return counterReportsByCounterName;
    }
    
    private HtmlCounterReport writeCounter(final Counter counter) throws IOException {
        this.writeCounterTitle(counter);
        final HtmlCounterReport htmlCounterReport = new HtmlCounterReport(counter, this.range, this.getWriter());
        htmlCounterReport.toHtml();
        return htmlCounterReport;
    }
    
    private void writeCounterTitle(final Counter counter) throws IOException {
        this.write("<h3><a name='" + counter.getName() + "'></a>");
        this.write("<img width='24' height='24' src='?resource=" + counter.getIconName() + "' alt='" + counter.getName() + "'/>");
        final String counterLabel = HtmlAbstractReport.getString(counter.getName() + "Label");
        this.write(HtmlAbstractReport.getFormattedString("Statistiques_compteur", counterLabel));
        this.writeln(" - " + this.range.getLabel() + "</h3>");
    }
    
    static void writeAddAndRemoveApplicationLinks(final String currentApplication, final Writer writer) throws IOException {
        new HtmlForms(writer).writeAddAndRemoveApplicationLinks(currentApplication);
    }
    
    void writeMessageIfNotNull(final String message, final String partToRedirectTo, final String anchorNameForRedirect) throws IOException {
        if (message != null) {
            this.writeln("<script type='text/javascript'>");
            this.writeDirectly("alert(\"" + HtmlAbstractReport.javascriptEncode(message) + "\");");
            this.writeln("");
            if (partToRedirectTo == null) {
                if (anchorNameForRedirect == null) {
                    this.writeln("location.href = '?'");
                }
                else {
                    this.writeln("if (location.href.indexOf('?') != -1) {");
                    this.writeDirectly("location.href = location.href.substring(0, location.href.indexOf('?')) + '#" + anchorNameForRedirect + "';");
                    this.writeln("} else {");
                    this.writeDirectly("location.href = '#" + anchorNameForRedirect + "';");
                    this.writeln("}");
                }
            }
            else {
                this.writeln("location.href = '?part=" + partToRedirectTo + '\'');
            }
            this.writeln("</script>");
        }
    }
    
    private void writeGraphs() throws IOException {
        this.writeGraphs(this.collector.getCounterJRobins());
        final Collection<JRobin> otherJRobins = this.collector.getOtherJRobins();
        if (!otherJRobins.isEmpty()) {
            this.writeln("<div align='right'>");
            this.writeShowHideLink("detailsGraphs", "#Autres_courbes#");
            this.writeln("</div>");
            this.writeln("<div id='detailsGraphs' style='display: none;'><div>");
            this.writeGraphs(otherJRobins);
            this.writeln("</div></div>");
        }
    }
    
    private void writeGraphs(final Collection<JRobin> jrobins) throws IOException {
        int i = 0;
        for (final JRobin jrobin : jrobins) {
            if (this.collector.isJRobinDisplayed(jrobin)) {
                final String jrobinName = jrobin.getName();
                this.writeln("<a href='?part=graph&amp;graph=" + jrobinName + "'><img class='synthese' src='?width=200&amp;height=" + 50 + "&amp;graph=" + jrobinName + "' alt=\"" + jrobin.getLabel() + "\" title=\"" + jrobin.getLabel() + "\"/></a>");
            }
            if (++i % 3 == 0) {
                this.writeln("<br/>");
            }
        }
    }
    
    private void writeCurrentRequests(final JavaInformations javaInformations, final List<Counter> counters, final Map<String, HtmlCounterReport> counterReportsByCounterName) throws IOException {
        final List<ThreadInformations> threadInformationsList = javaInformations.getThreadInformationsList();
        final boolean stackTraceEnabled = javaInformations.isStackTraceEnabled();
        final List<CounterRequestContext> rootCurrentContexts = this.collector.getRootCurrentContexts(counters);
        this.writeCurrentRequests(threadInformationsList, rootCurrentContexts, stackTraceEnabled, 500, false, counterReportsByCounterName);
    }
    
    void writeAllCurrentRequestsAsPart(final Map<JavaInformations, List<CounterRequestContext>> currentRequests) throws IOException {
        this.writeln("<div  class='noPrint'>");
        this.writeln("<a  href='javascript:history.back()'><img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        this.writeln("<a href='?part=currentRequests'><img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
        if (isPdfEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=currentRequests&amp;format=pdf' title='#afficher_PDF#'>");
            this.write("<img src='?resource=pdf.png' alt='#PDF#'/> #PDF#</a>");
        }
        this.writeln("</div><br/>");
        for (final Map.Entry<JavaInformations, List<CounterRequestContext>> entry : currentRequests.entrySet()) {
            final JavaInformations javaInformations = entry.getKey();
            final List<CounterRequestContext> rootCurrentContexts = entry.getValue();
            final List<ThreadInformations> threadInformationsList = javaInformations.getThreadInformationsList();
            final boolean stackTraceEnabled = javaInformations.isStackTraceEnabled();
            this.writeCurrentRequests(threadInformationsList, rootCurrentContexts, stackTraceEnabled, Integer.MAX_VALUE, true, null);
            this.write("<br/><br/>");
        }
    }
    
    private void writeCurrentRequests(final List<ThreadInformations> threadInformationsList, final List<CounterRequestContext> rootCurrentContexts, final boolean stackTraceEnabled, final int maxContextsDisplayed, final boolean onlyTitleAndDetails, final Map<String, HtmlCounterReport> counterReportsByCounterName) throws IOException {
        final HtmlCounterRequestContextReport htmlCounterRequestContextReport = new HtmlCounterRequestContextReport(rootCurrentContexts, counterReportsByCounterName, threadInformationsList, stackTraceEnabled, maxContextsDisplayed, this.getWriter());
        if (onlyTitleAndDetails) {
            htmlCounterRequestContextReport.writeTitleAndDetails();
        }
        else {
            htmlCounterRequestContextReport.toHtml();
        }
    }
    
    void writeAllThreadsAsPart() throws IOException {
        this.writeln("<div class='noPrint'>");
        this.writeln("<a href='javascript:history.back()'><img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");
        this.writeln("<a href='?part=threads'><img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
        this.writeln("</div> <br/>");
        this.writeTitle("threads.png", HtmlAbstractReport.getString("Threads"));
        this.write(" <br/>");
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            this.write(" <b>");
            this.writeDirectly(HtmlAbstractReport.getFormattedString("Threads_sur", javaInformations.getHost()));
            this.write(": </b>");
            this.writeln(HtmlAbstractReport.getFormattedString("thread_count", javaInformations.getThreadCount(), javaInformations.getPeakThreadCount(), javaInformations.getTotalStartedThreadCount()));
            final HtmlThreadInformationsReport htmlThreadInformationsReport = new HtmlThreadInformationsReport(javaInformations.getThreadInformationsList(), javaInformations.isStackTraceEnabled(), this.getWriter());
            htmlThreadInformationsReport.writeDeadlocks();
            this.writeln("<br/><br/>");
            htmlThreadInformationsReport.toHtml();
        }
    }
    
    void writeThreadsDump() throws IOException {
        this.writeDirectly(I18N.getCurrentDateAndTime());
        this.writeDirectly("\n\n");
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            this.writeDirectly("===== " + HtmlAbstractReport.getFormattedString("Threads_sur", javaInformations.getHost()) + " =====");
            this.writeDirectly("\n\n");
            final HtmlThreadInformationsReport htmlThreadInformationsReport = new HtmlThreadInformationsReport(javaInformations.getThreadInformationsList(), javaInformations.isStackTraceEnabled(), this.getWriter());
            htmlThreadInformationsReport.writeThreadsDump();
        }
    }
    
    private void writeThreads() throws IOException {
        int i = 0;
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            this.write("<b>");
            this.writeDirectly(HtmlAbstractReport.getFormattedString("Threads_sur", javaInformations.getHost()));
            this.write(": </b>");
            this.writeln(HtmlAbstractReport.getFormattedString("thread_count", javaInformations.getThreadCount(), javaInformations.getPeakThreadCount(), javaInformations.getTotalStartedThreadCount()));
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            final List<ThreadInformations> threadInformationsList = javaInformations.getThreadInformationsList();
            final HtmlThreadInformationsReport htmlThreadInformationsReport = new HtmlThreadInformationsReport(threadInformationsList, javaInformations.isStackTraceEnabled(), this.getWriter());
            if (threadInformationsList.size() <= 500) {
                final String id = "threads_" + i;
                this.writeShowHideLink(id, "#Details#");
                htmlThreadInformationsReport.writeDeadlocks();
                this.writeln("<br/><br/><div id='" + id + "' style='display: none;'>");
                htmlThreadInformationsReport.toHtml();
                this.writeln("</div><br/>");
            }
            else {
                this.writeln("<a href='?part=threads'>#Details#</a><br/>");
            }
            ++i;
        }
    }
    
    void writeCounterSummaryPerClass(final String counterName, final String requestId) throws IOException {
        final Counter counter = this.collector.getRangeCounter(this.range, counterName);
        this.writeln("<div class='noPrint'>");
        this.writeln("<a href='javascript:history.back()'><img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        final String hrefStart = "<a href='?part=counterSummaryPerClass&amp;counter=" + counter.getName() + ((requestId == null) ? "" : ("&amp;graph=" + HtmlAbstractReport.urlEncode(requestId)));
        this.writeln(hrefStart + "'>");
        this.writeln("<img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
        if (isPdfEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write(hrefStart);
            this.writeln("&amp;format=pdf' title='#afficher_PDF#'>");
            this.write("<img src='?resource=pdf.png' alt='#PDF#'/> #PDF#</a>");
        }
        this.writeln("</div>");
        this.writeCounterTitle(counter);
        final HtmlCounterReport htmlCounterReport = new HtmlCounterReport(counter, this.range, this.getWriter());
        htmlCounterReport.writeRequestsAggregatedOrFilteredByClassName(requestId);
    }
    
    private boolean isGcEnabled() {
        return Action.GC_ENABLED || this.collectorServer != null;
    }
    
    private boolean isHeapDumpEnabled() {
        return Action.HEAP_DUMP_ENABLED || this.collectorServer != null;
    }
    
    private boolean isHeapHistoEnabled() {
        return this.collectorServer != null || VirtualMachine.isEnabled();
    }
    
    private boolean isSamplingEnabled() {
        return this.collectorServer != null || this.collector.getSamplingProfiler() != null;
    }
    
    private boolean isDatabaseEnabled() {
        return !Parameters.isNoDatabase() && this.javaInformationsList.get(0).getDataBaseVersion() != null && !this.javaInformationsList.get(0).getDataBaseVersion().contains("Exception");
    }
    
    private boolean doesWebXmlExists() {
        return this.javaInformationsList.get(0).doesWebXmlExists();
    }
    
    private boolean isSessionsEnabled() {
        return this.javaInformationsList.get(0).getSessionCount() >= 0;
    }
    
    private boolean isCacheEnabled() {
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            if (javaInformations.isCacheEnabled()) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isJobEnabled() {
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            if (javaInformations.isJobEnabled()) {
                return true;
            }
        }
        return false;
    }
    
    private void writeCaches() throws IOException {
        int i = 0;
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            if (!javaInformations.isCacheEnabled()) {
                continue;
            }
            final List<CacheInformations> cacheInformationsList = javaInformations.getCacheInformationsList();
            this.writeln("<b>");
            this.writeln(HtmlAbstractReport.getFormattedString("caches_sur", cacheInformationsList.size(), javaInformations.getHost()));
            this.writeln("</b>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            final String id = "caches_" + i;
            this.writeShowHideLink(id, "#Details#");
            this.writeln("<br/><br/><div id='" + id + "' style='display: none;'><div>");
            new HtmlCacheInformationsReport(javaInformations.getCacheInformationsList(), this.getWriter()).toHtml();
            this.writeln("</div></div><br/>");
            ++i;
        }
    }
    
    private void writeJobs(final Counter rangeJobCounter) throws IOException {
        int i = 0;
        for (final JavaInformations javaInformations : this.javaInformationsList) {
            if (!javaInformations.isJobEnabled()) {
                continue;
            }
            final List<JobInformations> jobInformationsList = javaInformations.getJobInformationsList();
            this.writeln("<b>");
            this.writeln(HtmlAbstractReport.getFormattedString("jobs_sur", jobInformationsList.size(), javaInformations.getHost(), javaInformations.getCurrentlyExecutingJobCount()));
            this.writeln("</b>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            final String id = "job_" + i;
            this.writeShowHideLink(id, "#Details#");
            this.writeln("<br/><br/><div id='" + id + "' style='display: none;'><div>");
            new HtmlJobInformationsReport(javaInformations.getJobInformationsList(), rangeJobCounter, this.getWriter()).toHtml();
            this.writeln("</div></div><br/>");
            ++i;
        }
    }
    
    private void writeSystemActionsLinks() throws IOException {
        this.writeln("<div align='center' class='noPrint'>");
        final String separator = "&nbsp;&nbsp;&nbsp;&nbsp;";
        final String endOfOnClickConfirm = "');\">";
        if (this.isGcEnabled()) {
            this.write("<a href='?action=gc' onclick=\"javascript:return confirm('" + HtmlAbstractReport.getStringForJavascript("confirm_ramasse_miette") + "');\">");
            this.write("<img src='?resource=broom.png' width='20' height='20' alt='#ramasse_miette#' /> #ramasse_miette#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        else {
            this.write("<a href='' onclick=\"javascript:alert('" + HtmlAbstractReport.getStringForJavascript("ramasse_miette_desactive") + "');return false;\">");
            this.write("<img src='?resource=broom.png' width='20' height='20' alt='#ramasse_miette#' /> #ramasse_miette#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        if (this.isHeapDumpEnabled()) {
            this.write("<a href='?action=heap_dump' onclick=\"javascript:return confirm('" + HtmlAbstractReport.getStringForJavascript("confirm_heap_dump") + "');\">");
            this.write("<img src='?resource=heapdump.png' width='20' height='20' alt=\"#heap_dump#\" /> #heap_dump#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        if (this.isHeapHistoEnabled()) {
            this.write("<a href='?part=heaphisto'>");
            this.write("<img src='?resource=memory.png' width='20' height='20' alt=\"#heaphisto#\" /> #heaphisto#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        if (this.isSessionsEnabled()) {
            this.write("<a href='?action=invalidate_sessions' onclick=\"javascript:return confirm('" + HtmlAbstractReport.getStringForJavascript("confirm_invalidate_sessions") + "');\">");
            this.write("<img src='?resource=user-trash.png' width='18' height='18' alt=\"#invalidate_sessions#\" /> #invalidate_sessions#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=sessions'>");
            this.writeln("<img src='?resource=system-users.png' width='20' height='20' alt=\"#sessions#\" /> #sessions#</a>");
        }
        if (this.isSamplingEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=hotspots'>");
            this.writeln("<img src='?resource=clock.png' width='20' height='20' alt=\"#hotspots#\" /> #hotspots#</a>");
        }
        this.writeln("<br />");
        if (this.doesWebXmlExists()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=web.xml'>");
            this.write("<img src='?resource=xml.png' width='20' height='20' alt=\"#web.xml#\" /> #web.xml#</a>");
        }
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        this.write("<a href='?part=mbeans'>");
        this.write("<img src='?resource=mbeans.png' width='20' height='20' alt=\"#MBeans#\" /> #MBeans#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        this.write("<a href='?part=processes'>");
        this.write("<img src='?resource=processes.png' width='20' height='20' alt=\"#processes#\" /> #processes#</a>");
        final String serverInfo = this.javaInformationsList.get(0).getServerInfo();
        if (serverInfo != null && !serverInfo.contains("Winstone")) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=jndi'>");
            this.write("<img src='?resource=jndi.png' width='20' height='20' alt=\"#Arbre_JNDI#\" /> #Arbre_JNDI#</a>");
        }
        if (this.isDatabaseEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=connections'>");
            this.write("<img src='?resource=db.png' width='20' height='20' alt=\"#Connexions_jdbc_ouvertes#\" /> #Connexions_jdbc_ouvertes#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=database'>");
            this.writeln("<img src='?resource=db.png' width='20' height='20' alt=\"#database#\" /> #database#</a>");
        }
        this.writeln("<br/></div>");
    }
    
    private void writeApplicationsLinks() throws IOException {
        assert this.collectorServer != null;
        this.writeln("<div align='center'>");
        final Collection<String> applications = Parameters.getCollectorUrlsByApplications().keySet();
        if (applications.size() > 1 || !this.collectorServer.getLastCollectExceptionsByApplication().isEmpty()) {
            if (applications.size() > 10) {
                this.writeln("<table summary='applications'><tr><td>");
                this.writeShowHideLink("chooseApplication", "#Choix_application#");
                if (Parameters.getCollectorApplicationsFile().canWrite()) {
                    writeAddAndRemoveApplicationLinks(this.collector.getApplication(), this.getWriter());
                }
                this.writeln("<div id='chooseApplication' style='display: none;'><div>&nbsp;&nbsp;&nbsp;");
                this.writeApplicationsLinks(applications, "<br />&nbsp;&nbsp;&nbsp;");
                this.writeln("</div></div></td></tr></table>");
            }
            else {
                this.writeln("&nbsp;&nbsp;&nbsp;#Choix_application# :&nbsp;&nbsp;&nbsp;");
                this.writeApplicationsLinks(applications, "&nbsp;&nbsp;&nbsp;");
                if (Parameters.getCollectorApplicationsFile().canWrite()) {
                    writeAddAndRemoveApplicationLinks(this.collector.getApplication(), this.getWriter());
                }
            }
        }
        else if (Parameters.getCollectorApplicationsFile().canWrite()) {
            writeAddAndRemoveApplicationLinks(this.collector.getApplication(), this.getWriter());
        }
        this.writeln("</div>");
    }
    
    private void writeApplicationsLinks(final Collection<String> applications, final String separator) throws IOException {
        final Map<String, Throwable> lastCollectExceptionsByApplication = this.collectorServer.getLastCollectExceptionsByApplication();
        for (final String application : applications) {
            final Throwable lastCollectException = lastCollectExceptionsByApplication.get(application);
            this.writeln("<a href='?application=" + application + "' class='tooltip'>");
            if (lastCollectException == null) {
                this.writeln("<img src='?resource=bullets/green.png' alt='#Application_disponible#'/>");
                this.writeln("<em style='text-align: left; font-size: 10pt;'>");
                this.writeln("#Application_disponible#");
                this.writeln("</em>");
            }
            else {
                this.writeln("<img src='?resource=bullets/red.png' alt='#Application_indisponible#'/>");
                this.writeln("<em style='text-align: left; font-size: 10pt;'>");
                this.writeln("#Application_indisponible#:<br/>");
                this.writeDirectly(HtmlAbstractReport.htmlEncode(lastCollectException.toString()));
                this.writeDirectly("<br/>");
                for (final StackTraceElement stackTraceElement : lastCollectException.getStackTrace()) {
                    this.writeDirectly(HtmlAbstractReport.htmlEncode(stackTraceElement.toString()));
                    this.writeDirectly("<br/>");
                }
                this.writeln("</em>");
            }
            this.writeln(application + "</a>");
            this.writeln(separator);
        }
    }
    
    void writeRefreshAndPeriodLinks(final String graphName, final String part) throws IOException {
        this.writeln("<div class='noPrint'>");
        final String separator = "&nbsp;&nbsp;&nbsp;&nbsp;";
        final String graphParameter = "&amp;graph=";
        if (graphName == null) {
            this.write("<a href='?' title='#Rafraichir#'>");
        }
        else {
            this.write("<a href='javascript:history.back()'><img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.writeln("<a href='?'><img src='?resource=action_home.png' alt='#Page_principale#'/> #Page_principale#</a>");
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?part=" + part + "&amp;graph=" + HtmlAbstractReport.urlEncode(graphName) + "' title='#Rafraichir#'>");
        }
        this.write("<img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
        if (graphName == null && HtmlAbstractReport.isPdfEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
            this.write("<a href='?format=pdf' title='#afficher_PDF#'>");
            this.write("<img src='?resource=pdf.png' alt='#PDF#'/> #PDF#</a>");
        }
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        this.write("<a href='?resource=#help_url#' target='_blank'");
        this.write(" title=\"#Afficher_aide_en_ligne#\"><img src='?resource=action_help.png' alt='#Aide_en_ligne#'/> #Aide_en_ligne#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        this.write("<a href='?part=jnlp'");
        this.write(" title=\"#RDA#\"><img src='?resource=systemmonitor.png' width='16' height='16' alt='#RDA#'/> #Desktop#</a>");
        this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;");
        this.writeln("#Choix_periode# :&nbsp;");
        for (final Period myPeriod : Period.values()) {
            if (graphName == null) {
                this.write("<a href='?period=" + myPeriod.getCode() + "' ");
            }
            else {
                this.write("<a href='?part=" + part + "&amp;graph=" + HtmlAbstractReport.urlEncode(graphName) + "&amp;period=" + myPeriod.getCode() + "' ");
            }
            this.write("title='" + HtmlAbstractReport.getFormattedString("Choisir_periode", myPeriod.getLinkLabel()) + "'>");
            this.write("<img src='?resource=" + myPeriod.getIconName() + "' alt='" + myPeriod.getLinkLabel() + "' /> ");
            this.writeln(myPeriod.getLinkLabel() + "</a>&nbsp;");
        }
        new HtmlForms(this.getWriter()).writeCustomPeriodLink(this.range, graphName, part);
        this.writeln("</div>");
    }
    
    private void writeDurationAndOverhead() throws IOException {
        final long displayDuration = System.currentTimeMillis() - this.start;
        this.writeln("<a name='bottom'></a>");
        this.writeln("<br/><div style='font-size:10pt;'>");
        this.writeln("#temps_derniere_collecte#: " + this.collector.getLastCollectDuration() + " #ms#<br/>");
        this.writeln("#temps_affichage#: " + displayDuration + " #ms#<br/>");
        this.writeln("#Estimation_overhead_memoire#: < " + (this.collector.getEstimatedMemorySize() / 1024L / 1024L + 1L) + " #Mo#");
        this.writeln("<br/>#Usage_disque#: " + (this.collector.getDiskUsage() / 1024L / 1024L + 1L) + " #Mo#");
        if (Parameters.isSystemActionsEnabled()) {
            this.writeln("&nbsp;&nbsp;&nbsp;<a href='?action=purge_obsolete_files' class='noPrint'>");
            this.writeln("<img width='14' height='14' src='?resource=user-trash.png' alt='#Purger_les_fichiers_obsoletes#' title='#Purger_les_fichiers_obsoletes#'/></a>");
        }
        if (Parameters.JAVAMELODY_VERSION != null) {
            this.writeln("<br/><br/>JavaMelody " + Parameters.JAVAMELODY_VERSION);
        }
        if (this.collectorServer == null) {
            this.writeln("<br/>");
            this.writeShowHideLink("debuggingLogs", "Debugging logs");
            this.writeln("<br/><br/>");
            this.writeln("<div id='debuggingLogs' style='display: none;'>");
            final List<String> debuggingLogs = LOG.getDebuggingLogs();
            if (debuggingLogs.size() >= 50) {
                this.writeln("<div class='severe'>Only the last 50 messages are displayed</div>");
            }
            for (final String msg : debuggingLogs) {
                this.writeDirectly(HtmlAbstractReport.htmlEncodeButNotSpace(msg).replaceAll("[\t]", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"));
                this.writeln("<br/>");
            }
            this.writeln("</div>");
        }
        this.writeln("</div>");
    }
    
    private void writePoweredBy() throws IOException {
        this.writeln("");
    }
    
    private static class HtmlForms extends HtmlAbstractReport
    {
        HtmlForms(final Writer writer) {
            super(writer);
        }
        
        void writeCustomPeriodLink(final Range range, final String graphName, final String part) throws IOException {
            this.writeln("<a href=\"javascript:showHide('customPeriod');document.customPeriodForm.startDate.focus();\" ");
            final String linkLabel = HtmlAbstractReport.getString("personnalisee");
            this.writeln("title='" + HtmlAbstractReport.getFormattedString("Choisir_periode", linkLabel) + "'>");
            this.writeln("<img src='?resource=calendar.png' alt='#personnalisee#' /> #personnalisee#</a>");
            this.writeln("<div id='customPeriod' style='display: none;'>");
            this.writeln("<script type='text/javascript'>");
            this.writeln("function validateCustomPeriodForm() {");
            this.writeln("   periodForm = document.customPeriodForm;");
            this.writelnCheckMandatory("periodForm.startDate", "dates_mandatory");
            this.writelnCheckMandatory("periodForm.endDate", "dates_mandatory");
            this.writeln("   periodForm.period.value=periodForm.startDate.value + '|' + periodForm.endDate.value;");
            this.writeln("   return true;");
            this.writeln("}");
            this.writeln("</script>");
            this.writeln("<br/><br/>");
            final DateFormat dateFormat = I18N.createDateFormat();
            String dateFormatPattern;
            if (HtmlAbstractReport.getString("dateFormatPattern").length() == 0) {
                final String pattern = ((SimpleDateFormat)dateFormat).toPattern();
                dateFormatPattern = pattern.toLowerCase(I18N.getCurrentLocale());
            }
            else {
                dateFormatPattern = HtmlAbstractReport.getString("dateFormatPattern");
            }
            this.writeln("<form name='customPeriodForm' method='get' action='' onsubmit='return validateCustomPeriodForm();'>");
            this.writeln("<br/><b>#startDate#</b>&nbsp;&nbsp;<input type='text' size='10' name='startDate' ");
            if (range.getStartDate() != null) {
                this.writeln("value='" + dateFormat.format(range.getStartDate()) + '\'');
            }
            this.writeln("/>&nbsp;&nbsp;<b>#endDate#</b>&nbsp;&nbsp;<input type='text' size='10' name='endDate' ");
            if (range.getEndDate() != null) {
                this.writeln("value='" + dateFormat.format(range.getEndDate()) + '\'');
            }
            this.writeln("/>&nbsp;&nbsp;");
            this.writeDirectly('(' + dateFormatPattern + ')');
            this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type='submit' value='#ok#'/><br/><br/>");
            this.writeln("<input type='hidden' name='period' value=''/>");
            if (graphName != null) {
                this.writeln("<input type='hidden' name='part' value='" + part + "'/>");
                this.writeln("<input type='hidden' name='graph' value='" + HtmlAbstractReport.urlEncode(graphName) + "'/>");
            }
            this.writeln("</form><br/>");
            this.writeln("</div>");
        }
        
        void writeAddAndRemoveApplicationLinks(final String currentApplication) throws IOException {
            if (currentApplication == null) {
                this.writeln("<div align='center'><h3>#add_application#</h3>");
                this.writeln("#collect_server_intro#");
            }
            else {
                final String separator = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                this.writeln("<a href=\"javascript:showHide('addApplication');document.appForm.appName.focus();\"");
                this.writeln(" class='noPrint'><img src='?resource=action_add.png' alt='#add_application#'/> #add_application#</a>");
                this.writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                this.writeln("<a href='?action=remove_application&amp;application=" + currentApplication + "' class='noPrint' ");
                final String messageConfirmation = HtmlAbstractReport.getFormattedString("confirm_remove_application", currentApplication);
                this.writeln("onclick=\"javascript:return confirm('" + HtmlAbstractReport.javascriptEncode(messageConfirmation) + "');\">");
                final String removeApplicationLabel = HtmlAbstractReport.getFormattedString("remove_application", currentApplication);
                this.writeln("<img src='?resource=action_delete.png' alt=\"" + removeApplicationLabel + "\"/> " + removeApplicationLabel + "</a>");
                this.writeln("<div id='addApplication' style='display: none;'>");
            }
            this.writeln("<script type='text/javascript'>");
            this.writeln("function validateAppForm() {");
            this.writelnCheckMandatory("document.appForm.appName", "app_name_mandatory");
            this.writelnCheckMandatory("document.appForm.appUrls", "app_urls_mandatory");
            this.writeln("   return true;");
            this.writeln("}");
            this.writeln("</script>");
            this.writeln("<br/> <br/>");
            this.writeln("<form name='appForm' method='post' action='' onsubmit='return validateAppForm();'>");
            this.writeln("<br/><b>#app_name_to_monitor# :</b>&nbsp;&nbsp;<input type='text' size='15' name='appName'/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            this.writeln("<b>#app_urls# :</b>&nbsp;&nbsp;<input type='text' size='50' name='appUrls'/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            this.writeln("<input type='submit' value='#add#'/><br/>");
            this.writeln("#urls_sample# : <i>http://myhost/myapp/</i> #or# <i>http://host1/myapp/,http://host2/myapp/</i>");
            this.writeln("<br/> <br/>");
            this.writeln("</form>");
            this.writeln("</div>\n");
        }
        
        private void writelnCheckMandatory(final String fieldFullName, final String msgKey) throws IOException {
            this.writeln("   if (" + fieldFullName + ".value.length == 0) {");
            this.writeln("      alert('" + HtmlAbstractReport.getStringForJavascript(msgKey) + "');");
            this.writeln("      " + fieldFullName + ".focus();");
            this.writeln("      return false;");
            this.writeln("   }");
        }
        
        @Override
        void toHtml() {
            throw new UnsupportedOperationException();
        }
    }
}
