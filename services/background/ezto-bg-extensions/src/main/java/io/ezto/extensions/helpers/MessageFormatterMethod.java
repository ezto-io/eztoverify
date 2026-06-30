package io.ezto.extensions.helpers;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class MessageFormatterMethod implements TemplateMethodModelEx {

    private final Properties messages;
    private final Locale locale;

    public MessageFormatterMethod(Locale locale, Properties messages) {
        this.locale = locale;
        this.messages = messages;
    }
    @Override
    public Object exec(List list) throws TemplateModelException {
        if (!list.isEmpty()) {
            // resolve any remaining ${} expressions
            List<Object> resolved = resolve(list.subList(1, list.size()));
            String key = list.get(0).toString();
            return new MessageFormat(messages.getProperty(key,key),locale).format(resolved.toArray());
        } else {
            return null;
        }
    }

    private List<Object> resolve(List<Object> list) {
        ArrayList<Object> result = new ArrayList<>();
        for (Object item: list) {
            if (item instanceof SimpleScalar) {
                item = ((SimpleScalar) item).getAsString();
            }
            if (item instanceof String) {
                result.add(resolveVariables((String) item, messages,"${", "}"));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    public static String resolveVariables(String text, Properties props, String startMarker, String endMarker) {

        int e = 0;
        int s = text.indexOf(startMarker);
        if (s == -1) {
            return text;
        } else {
            StringBuilder sb = new StringBuilder();

            do {
                if (e < s) {
                    sb.append(text.substring(e, s));
                }
                e = text.indexOf(endMarker, s + startMarker.length());
                if (e != -1) {
                    String key = text.substring(s + startMarker.length(), e);
                    sb.append(props.getProperty(key, key));
                    e += endMarker.length();
                    s = text.indexOf(startMarker, e);
                } else {
                    e = s;
                    break;
                }
            } while (s != -1);

            if (e < text.length()) {
                sb.append(text.substring(e));
            }
            return sb.toString();
        }
    }
}
