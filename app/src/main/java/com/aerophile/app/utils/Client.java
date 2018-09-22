package com.aerophile.app.utils;

import org.androidannotations.rest.spring.annotations.Body;
import org.androidannotations.rest.spring.annotations.Path;
import org.androidannotations.rest.spring.annotations.Post;
import org.androidannotations.rest.spring.annotations.Rest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;

/**
 * Created by Nicolas on 11/01/2016.
 */
@Rest(rootUrl = "https://www.fev.aerophile.com", converters = { FormHttpMessageConverter.class, StringHttpMessageConverter.class } )
public interface Client {

    @Post("/index.php?{typeDonnees}&lang={langue}")
    String envoieJournee(@Body MultiValueMap<String, Object> data, @Path String typeDonnees, @Path String langue);
}