// 
// Decompiled by Procyon v0.5.36
// 

package ratpack.error.internal;

import com.google.common.html.HtmlEscapers;
import java.io.OutputStream;
import io.netty.buffer.ByteBuf;
import ratpack.http.internal.HttpHeaderConstants;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufAllocator;
import ratpack.handling.Context;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import java.util.function.Consumer;
import java.io.InputStream;
import java.io.IOException;
import com.google.common.io.CharStreams;
import java.io.InputStreamReader;
import io.netty.util.CharsetUtil;
import com.google.common.escape.Escaper;

public abstract class ErrorPageRenderer
{
    private static final Escaper HTML_ESCAPER;
    private String style;
    
    public ErrorPageRenderer() {
        if (this.style == null) {
            final InputStream resourceAsStream = ErrorPageRenderer.class.getResourceAsStream("error-template-style.css");
            if (resourceAsStream == null) {
                throw new IllegalStateException("Couldn't find style resource");
            }
            final InputStreamReader reader = new InputStreamReader(resourceAsStream, CharsetUtil.decoder(CharsetUtil.UTF_8));
            try {
                this.style = CharStreams.toString((Readable)reader);
            }
            catch (IOException e) {
                throw new IllegalStateException("Could not read style stream", e);
            }
        }
        this.render();
    }
    
    protected abstract void render();
    
    protected void stack(final BodyWriter w, final String heading, final Throwable throwable) {
        if (heading != null) {
            w.println("<div id=\"stack-header\">").println("<div class=\"wrapper\">").print("<h3>").escape(heading).print("</h3>").println("</div>").println("</div>");
        }
        w.println("<div class=\"wrapper\">").println("<div class=\"stack\">");
        w.print("<pre><code>");
        this.throwable(w, throwable, false);
        w.println("</pre></code>").println("</div>").println("</div>");
    }
    
    protected void throwable(final BodyWriter w, final Throwable throwable, final boolean isCause) {
        if (throwable != null) {
            if (isCause) {
                w.escape("Caused by: ");
            }
            w.println(throwable.toString());
            for (final StackTraceElement ste : throwable.getStackTrace()) {
                final String className = ste.getClassName();
                if (className.startsWith("ratpack") || className.startsWith("io.netty") || className.startsWith("com.google") || className.startsWith("java") || className.startsWith("org.springsource.loaded")) {
                    w.print("<span class='stack-core'>  at ").escape(ste.toString()).println("</span>");
                }
                else {
                    w.print("  at ").escape(ste.toString()).println("");
                }
            }
            this.throwable(w, throwable.getCause(), true);
        }
    }
    
    protected void messages(final BodyWriter writer, final String heading, final Runnable block) {
        writer.println("<div class=\"wrapper\">").println("<div id=\"messages\">").println("<section>").print("<h2>").escape(heading).println("</h2>");
        block.run();
        writer.println("<p><em>Note: This page will only be visible during development.</em></p>").println("</section>").println("</div>").println("</div>");
    }
    
    protected void meta(final BodyWriter w, final Consumer<ImmutableMap.Builder<String, Object>> meta) {
        final ImmutableMap.Builder<String, Object> builder = (ImmutableMap.Builder<String, Object>)ImmutableMap.builder();
        meta.accept(builder);
        w.println("<table class=\"meta\">");
        for (final Map.Entry<String, Object> entry : builder.build().entrySet()) {
            w.print("<tr><th>").escape(entry.getKey()).print("</th><td>").escape(entry.getValue().toString()).println("</td></tr>");
        }
        w.println("</table>");
    }
    
    protected void render(final Context context, final String pageTitle, final Consumer<? super BodyWriter> body) {
        final ByteBuf buffer = context.get(ByteBufAllocator.class).buffer();
        final OutputStream out = (OutputStream)new ByteBufOutputStream(buffer);
        final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out, CharsetUtil.encoder(CharsetUtil.UTF_8)));
        final BodyWriter writer = new BodyWriter(printWriter);
        writer.println("<!DOCTYPE html>").println("<html>").println("<head>").print("  <title>").escape(pageTitle).println("</title>").println("    <style type=\"text/css\">").println(this.style).println("    </style>").println("</head>").println("<body>").println("  <header>").println("    <div class=\"logo\">").println("      <div class=\"martini\">").println("        <h1>Ratpack</h1>").println("      </div>").println("      <p>Development error page</p>").println("    </div>").println("  </header>");
        body.accept(writer);
        writer.println("<footer>").println("  <a href=\"http://www.ratpack.io\">Ratpack.io</a>").println("</footer>").println("</body>").println("</html>");
        printWriter.close();
        context.getResponse().send(HttpHeaderConstants.HTML_UTF_8, buffer);
    }
    
    static {
        HTML_ESCAPER = HtmlEscapers.htmlEscaper();
    }
    
    protected static class BodyWriter
    {
        private final PrintWriter writer;
        
        private BodyWriter(final PrintWriter writer) {
            this.writer = writer;
        }
        
        BodyWriter print(final String string) {
            this.writer.print(string);
            return this;
        }
        
        BodyWriter println(final String string) {
            this.writer.println(string);
            return this;
        }
        
        BodyWriter escape(final String string) {
            return this.print(ErrorPageRenderer.HTML_ESCAPER.escape(string));
        }
    }
}
