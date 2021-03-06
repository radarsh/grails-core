/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.rest.web

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.servlet.ModelAndView

import spock.lang.Specification

@TestFor(BookController)
@Mock(Book)
class RespondMethodSpec extends Specification{

    void setup() {
        def ga = grailsApplication
        ga.config.grails.mime.types =
            [ html: ['text/html','application/xhtml+xml'],
            xml: ['text/xml', 'application/xml'],
            text: 'text/plain',
            js: 'text/javascript',
            rss: 'application/rss+xml',
            atom: 'application/atom+xml',
            css: 'text/css',
            csv: 'text/csv',
            all: '*/*',
            json: ['application/json','text/json'],
            form: 'application/x-www-form-urlencoded',
            multipartForm: 'multipart/form-data'
        ]

        defineBeans {
            mimeTypes(MimeTypesFactoryBean) {
                grailsApplication = ga
            }
        }
    }

    void "Test that the respond method produces the correct model for a domain instance and no specific content type"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            webRequest.actionName = 'show'
            controller.show(book)
            def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
            modelAndView != null
            modelAndView instanceof ModelAndView
            modelAndView.model.book == book
            modelAndView.viewName == 'show'
    }

    void "Test that the respond method produces XML for a domain instance and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml;charset=UTF-8'
            response.xml.title.text() == 'The Stand'
    }

    void "Test that the respond method produces XML for a list of domains and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'
            def result = controller.index()

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml;charset=UTF-8'
    }

    void "Test that the respond method produces errors XML for a domain instance that has errors and a content type of XML"() {
        given:"A book instance"
            def book = new Book(title: "")
            book.validate()

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'text/xml;charset=UTF-8'
            response.xml.error.message.text() == 'Property [title] of class [class grails.rest.web.Book] cannot be null'
    }

    void "Test that the respond method produces JSON for a domain instance and a content type of JSON"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'json'

        def result = controller.show(book)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'application/json;charset=UTF-8'
            response.json.title == 'The Stand'
    }

    void "Test that the respond method produces a 415 for a format not supported"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'xml'

            def result = controller.showWithFormats(book.id)

        then:"A modelAndView and view is produced"
            response.status == 415
    }

    void "Test that the respond method produces JSON for an action that specifies explicit formats"() {
        given:"A book instance"
            def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
            response.format = 'json'

            def result = controller.showWithFormats(book.id)

        then:"A modelAndView and view is produced"
            result == null
            response.contentType == 'application/json;charset=UTF-8'
            response.json.title == 'The Stand'
    }

    void "Test that the respond method produces the correct model for a domain instance and content type is HTML"() {
        given:"A book instance"
        def book = new Book(title: "The Stand").save(flush:true)

        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.showWithModel(book)
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model == [book: book, extra: true]
        modelAndView.viewName == 'showWithModel'
    }

    void "Test that the respond method produces errors HTML for a domain instance that has errors and a content type of HTML"() {
        given:"A book instance"
        def book = new Book(title: "")
        book.validate()

        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.showWithModel(book)
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model == [book: book, extra: true]
        modelAndView.viewName == 'showWithModel'
    }
    
    void "Test that proxyHandler is used for unwrapping wrapped model"() {
        given:"A book instance"
        def book = new Book(title: "")
        book.validate()
        applicationContext.getBean("instanceControllersRestApi").proxyHandler = new TestProxyHandler()
        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.respond(new BookProxy(book: book), model: [extra: true])
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model == [book: book, extra: true]
        modelAndView.viewName == 'showWithModel'
    }
    
    void "Test that proxyHandler is used for unwrapping proxy collections"() {
        given:"A book instance"
        def book = new Book(title: "")
        book.validate()
        ['instanceControllersRestApi','rendererRegistry'].each { beanName ->
            applicationContext.getBean(beanName).proxyHandler = new TestProxyHandler()
        }
        def renderer=applicationContext.getBean('rendererRegistry').findRenderer(MimeType.HTML, [])
        renderer.proxyHandler = new TestProxyHandler()
        
        when:"The respond method is used to render a response"
        webRequest.actionName = 'showWithModel'
        controller.respond([new BookProxy(book: book)], model: [extra: true])
        def modelAndView = webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:"A modelAndView and view is produced"
        modelAndView != null
        modelAndView instanceof ModelAndView
        modelAndView.model.containsKey('bookList')
        modelAndView.model.extra == true
        modelAndView.viewName == 'showWithModel'
    }
}

@Artefact("Controller")
class BookController {
    def show(Book b) {
        respond b
    }

    def showWithModel(Book b) {
        respond b, model: [extra: true]
    }

    def index() {
        respond Book.list()
    }

    def showWithFormats(Long id) {
        respond Book.get(id), formats:['json', 'html']
    }
}
@Entity
class Book {
    String title

    static constraints = {
        title blank:false
    }
}

class BookProxy {
    Book book
}

class TestProxyHandler implements ProxyHandler {
    @Override
    public boolean isProxy(Object o) {
        false
    }

    @Override
    public Object unwrapIfProxy(Object instance) {
        if(instance instanceof BookProxy)
            return instance.book
        instance
    }

    @Override
    public boolean isInitialized(Object o) {
        true
    }

    @Override
    public void initialize(Object o) {
        
    }

    @Override
    public boolean isInitialized(Object obj, String associationName) {
        true
    }
}