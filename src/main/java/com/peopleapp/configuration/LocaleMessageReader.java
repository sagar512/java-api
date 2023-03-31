package com.peopleapp.configuration;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

/**
 * This class will read messages from local messages.properties file.
 */
@Component
public class LocaleMessageReader {

    private final MessageSourceAccessor accessor;

    public LocaleMessageReader(MessageSource messageSource) {
        this.accessor = new MessageSourceAccessor(messageSource, LocaleContextHolder.getLocale());
    }

    public String get(String code, Object[] messageParam) {
        return accessor.getMessage(code, messageParam);
    }

    public String get(String code) {
        return accessor.getMessage(code);
    }

}
