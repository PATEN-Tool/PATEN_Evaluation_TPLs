// 
// Decompiled by Procyon v0.5.36
// 

package eu.hinsch.spring.boot.actuator.logview;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.util.Assert;
import java.util.Iterator;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import java.io.FileInputStream;
import java.io.File;
import javax.servlet.ServletOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.nio.file.Paths;
import org.springframework.web.bind.annotation.ResponseBody;
import freemarker.template.TemplateException;
import java.nio.file.Path;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import java.net.URLEncoder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import freemarker.template.Configuration;
import java.util.List;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;

public class LogViewEndpoint implements MvcEndpoint
{
    private final List<FileProvider> fileProviders;
    private final Configuration freemarkerConfig;
    private final String loggingPath;
    private final List<String> stylesheets;
    
    public LogViewEndpoint(final String loggingPath, final List<String> stylesheets) {
        this.loggingPath = loggingPath;
        this.stylesheets = stylesheets;
        this.fileProviders = Arrays.asList(new FileSystemFileProvider(), new ZipArchiveFileProvider(), new TarGzArchiveFileProvider());
        (this.freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)).setClassForTemplateLoading((Class)this.getClass(), "/templates");
    }
    
    @RequestMapping
    public void redirect(final HttpServletResponse response) throws IOException {
        response.sendRedirect("log/");
    }
    
    @RequestMapping({ "/" })
    @ResponseBody
    public String list(final Model model, @RequestParam(required = false, defaultValue = "FILENAME") final SortBy sortBy, @RequestParam(required = false, defaultValue = "false") final boolean desc, @RequestParam(required = false) final String base) throws IOException, TemplateException {
        this.securityCheck(base);
        final Path currentFolder = this.loggingPath(base);
        final List<FileEntry> files = this.getFileProvider(currentFolder).getFileEntries(currentFolder);
        final List<FileEntry> sortedFiles = this.sortFiles(files, sortBy, desc);
        model.addAttribute("sortBy", (Object)sortBy);
        model.addAttribute("desc", (Object)desc);
        model.addAttribute("files", (Object)sortedFiles);
        model.addAttribute("currentFolder", (Object)currentFolder.toAbsolutePath().toString());
        model.addAttribute("base", (Object)((base != null) ? URLEncoder.encode(base, "UTF-8") : ""));
        model.addAttribute("parent", (Object)this.getParent(currentFolder));
        model.addAttribute("stylesheets", (Object)this.stylesheets);
        return FreeMarkerTemplateUtils.processTemplateIntoString(this.freemarkerConfig.getTemplate("logview.ftl"), (Object)model);
    }
    
    private FileProvider getFileProvider(final Path folder) {
        final RuntimeException ex;
        return this.fileProviders.stream().filter(provider -> provider.canHandle(folder)).findFirst().orElseThrow(() -> {
            new RuntimeException("no file provider found for " + folder.toString());
            return ex;
        });
    }
    
    private String getParent(final Path loggingPath) {
        final Path basePath = this.loggingPath(null);
        String parent = "";
        if (!basePath.toString().equals(loggingPath.toString())) {
            parent = loggingPath.getParent().toString();
            if (parent.startsWith(basePath.toString())) {
                parent = parent.substring(basePath.toString().length());
            }
        }
        return parent;
    }
    
    private Path loggingPath(final String base) {
        return (base != null) ? Paths.get(this.loggingPath, base) : Paths.get(this.loggingPath, new String[0]);
    }
    
    private List<FileEntry> sortFiles(final List<FileEntry> files, final SortBy sortBy, final boolean desc) {
        Comparator<FileEntry> comparator = null;
        switch (sortBy) {
            case FILENAME: {
                comparator = ((a, b) -> a.getFilename().compareTo(b.getFilename()));
                break;
            }
            case SIZE: {
                comparator = ((a, b) -> Long.compare(a.getSize(), b.getSize()));
                break;
            }
            case MODIFIED: {
                comparator = ((a, b) -> Long.compare(a.getModified().toMillis(), b.getModified().toMillis()));
                break;
            }
        }
        final List<FileEntry> sortedFiles = files.stream().sorted((Comparator<? super Object>)comparator).collect((Collector<? super Object, ?, List<FileEntry>>)Collectors.toList());
        if (desc) {
            Collections.reverse(sortedFiles);
        }
        return sortedFiles;
    }
    
    @RequestMapping({ "/view" })
    public void view(@RequestParam final String filename, @RequestParam(required = false) final String base, @RequestParam(required = false) final Integer tailLines, final HttpServletResponse response) throws IOException {
        this.securityCheck(filename);
        response.setContentType("text/plain");
        final Path path = this.loggingPath(base);
        final FileProvider fileProvider = this.getFileProvider(path);
        if (tailLines != null) {
            fileProvider.tailContent(path, filename, (OutputStream)response.getOutputStream(), tailLines);
        }
        else {
            fileProvider.streamContent(path, filename, (OutputStream)response.getOutputStream());
        }
    }
    
    @RequestMapping({ "/search" })
    public void search(@RequestParam final String term, final HttpServletResponse response) throws IOException {
        final Path folder = this.loggingPath(null);
        final List<FileEntry> files = this.getFileProvider(folder).getFileEntries(folder);
        final List<FileEntry> sortedFiles = this.sortFiles(files, SortBy.MODIFIED, false);
        final ServletOutputStream outputStream = response.getOutputStream();
        sortedFiles.stream().filter(file -> file.getFileType().equals(FileType.FILE)).forEach(file -> this.searchAndStreamFile(file, term, (OutputStream)outputStream));
    }
    
    private void searchAndStreamFile(final FileEntry fileEntry, final String term, final OutputStream outputStream) {
        final Path folder = this.loggingPath(null);
        try {
            final List<String> lines = (List<String>)IOUtils.readLines((InputStream)new FileInputStream(new File(folder.toFile().toString(), fileEntry.getFilename()))).stream().filter(line -> line.contains(term)).map(line -> "[" + fileEntry.getFilename() + "] " + line).collect(Collectors.toList());
            for (final String line2 : lines) {
                outputStream.write(line2.getBytes());
                outputStream.write(System.lineSeparator().getBytes());
            }
        }
        catch (IOException e) {
            throw new RuntimeException("error reading file", e);
        }
    }
    
    private void securityCheck(final String filename) {
        Assert.doesNotContain(filename, "..");
    }
    
    public String getPath() {
        return "/log";
    }
    
    public boolean isSensitive() {
        return true;
    }
    
    public Class<? extends Endpoint> getEndpointType() {
        return null;
    }
}
