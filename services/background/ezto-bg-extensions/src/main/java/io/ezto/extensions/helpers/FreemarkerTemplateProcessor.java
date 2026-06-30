package io.ezto.extensions.helpers;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.helpers.EnvironmentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FreemarkerTemplateProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FreemarkerTemplateProcessor.class);
    private final KeycloakSanitizerMethod kcSanitizeMethod = new KeycloakSanitizerMethod();
    private static Configuration configuration = null;
    private static final String MESSAGE_ROOT = "messages/" + EnvironmentReader.getValue("CUSTOMER","ezto") +
            "/messages";
    public static final Pattern DETECT_ENCODING_PATTERN = Pattern.compile("^#\\s*encoding:\\s*([\\w.:-]+)",
            Pattern.CASE_INSENSITIVE);
    public static final Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1;

    Locale locale = null;

    public FreemarkerTemplateProcessor(Locale lc) {
        this.locale = lc;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL[] additionalClasspathElements = ((URLClassLoader) classLoader).getURLs();
        Class<?> clazz = null;

        for (URL element : additionalClasspathElements) {
            try {
                URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{element}, classLoader);
                clazz = Class.forName("io.ezto.email.job.EmailJobProvider", true, urlClassLoader);
                break;
            } catch (ClassNotFoundException e) {
                LOG.error(e.toString());
                return;
            }
        }

        try{
            configuration = new Configuration(Configuration.VERSION_2_3_22);
            configuration.setClassForTemplateLoading(clazz, "/templates/" + EnvironmentReader.getValue("CUSTOMER",
                    "ezto"));
            Map<String, Object> sharedVariables = new HashMap<>();
            sharedVariables.put("msg", new MessageFormatterMethod(locale,getMessages(locale)));
            sharedVariables.put("kcSanitize", kcSanitizeMethod);
            sharedVariables.put("linkExpirationFormatter",new LinkExpirationFormatterMethod(getMessages(locale),locale));
            configuration.setSharedVariables(sharedVariables);
            configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        } catch (Exception e){
            LOG.error("ERROR Creating freemarker instance: " + e);
        }
    }

    public static FreemarkerTemplateProcessor getInstance(Locale locale) {
        return new FreemarkerTemplateProcessor(locale);
    }

    public String getHtmlBody(IEmailTemplate emailTemplate) {
        String htmlMessage;
        Template template = Template.getPlainTextTemplate("", emailTemplate.getPlaceHolders().get("htmlBody"), null);
        StringWriter writer = new StringWriter();
        try {
            HashMap<String, Object> data = new HashMap<>(emailTemplate.getPlaceHolders());
            data.put("dir", Locale.ENGLISH.getLanguage().equalsIgnoreCase("ar") ? "rtl" : "ltr");
            template.process(data, writer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        htmlMessage = writer.toString();
        htmlMessage = processTemplate(emailTemplate.getPlaceHolders(), htmlMessage);
        return htmlMessage;
    }

    public String processTemplate(Map<String,String> data,String htmlBody){
        if (data instanceof Map) {
            ((Map)data).put("kcSanitize", kcSanitizeMethod);
        }

        for (String key : data.keySet()){
            Object val = data.get(key);
            htmlBody = htmlBody.replace("{{"+key+"}}",val.toString());
        }
        return htmlBody;
    }

    public String processTemplate(String templateName, Map<String,String> dataModel) {
        String templatePath = String.format("html/%s", templateName);
        configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);
        Template template = null;
        try {
            template = configuration.getTemplate(templatePath);
        } catch (IOException e) {
            LOG.error("ERROR processing freemarker template: "+e.toString());
            throw new RuntimeException(e);
        }

        StringWriter writer = new StringWriter();
        try {
            HashMap<String, Object> data = new HashMap<>(dataModel);
            data.put("dir", locale.getLanguage().equalsIgnoreCase("ar") ? "rtl" : "ltr");
            template.process(data, writer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return writer.toString();
    }

    public Properties getMessages(Locale locale) throws IOException {
        if(locale == null){
            return new Properties();
        }
        Properties m = new Properties();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url =  classLoader.getResource(MESSAGE_ROOT +"_"+locale.getLanguage()+ ".properties");
        if (url != null) {
            Charset encoding = detectEncoding(url.openStream());
            if (locale.getLanguage().equalsIgnoreCase("ar")) {
                encoding = StandardCharsets.UTF_8;
            }
            try (Reader reader = new InputStreamReader(url.openStream(), encoding)) {
                m.load(reader);
            }
        }
        return m;
    }

    private static Charset detectEncoding(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, DEFAULT_ENCODING))) {
            String firstLine = br.readLine();
            if (firstLine != null) {
                Matcher matcher = DETECT_ENCODING_PATTERN.matcher(firstLine);
                if (matcher.find()) {
                    String encoding = matcher.group(1);
                    if (Charset.isSupported(encoding)) {
                        return Charset.forName(encoding);
                    } else {
                        LOG.warn("Unsupported encoding: {0}", encoding);
                    }
                }
            }
        }
        return DEFAULT_ENCODING;
    }

    public String getSubject(String subjectKey, List<Object> subjectAttributes) throws IOException {
        Object message = getMessages(locale).get(subjectKey);
        if(message != null){
            if(subjectAttributes == null || subjectAttributes.isEmpty())
                return message.toString();

            return MessageFormat.format(message.toString(), subjectAttributes.toArray()).replaceAll("[\\[\\]]", "");
        }
        return subjectKey;
    }
    public String getSubject(String subjectKey) throws IOException {
        Object message = getMessages(locale).get(subjectKey);
        if(message != null){
            return message.toString();
        }
        return subjectKey;
    }
}
