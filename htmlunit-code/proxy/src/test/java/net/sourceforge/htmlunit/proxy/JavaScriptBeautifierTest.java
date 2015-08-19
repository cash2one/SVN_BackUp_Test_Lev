/*
 * Copyright (c) 2010 HtmlUnit team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.htmlunit.proxy;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
/**
 * Test for {@link JavaScriptBeautifier}.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5491 $
 */
public class JavaScriptBeautifierTest {

    /**
     * Basic test.
     */
    @Test
    public void test() {
        final String source = "function oa(){if(!c.isReady){try{s.documentElement.doScroll(\"left\");}catch(a){"
            + "setTimeout(oa,1);return;}c.ready();}}"
            + "function La(a,b){b.src?c.ajax({url:b.src,async:false,dataType:\"script\"}):"
            + "c.globalEval(b.text||b.textContent||b.innerHTML||\"\");b.parentNode&&b.parentNode.removeChild(b);}"
            + "function $(a,b,d,f,e,i){var j=a.length;if(typeof b===\"object\"){for(var o in b)$(a,o,b[o],f,e,d);"
            + "return a;}if(d!==w){f=!i&&f&&c.isFunction(d);for(o=0;o<j;o++)e(a[o],b,f?d.call(a[o],o,e(a[o],b)):d,i);"
            + "return a;}return j?e(a[0],b):null;}function K(){return(new Date()).getTime();}function aa(){"
            + "return false;}"
            + "function ba(){return true;}function pa(a,b,d){d[0].type=a;return c.event.handle.apply(b,d);}"
            + "function qa(a){var b=true,d=[],f=[],e=arguments,i,j,o,p,n,t=c.extend({},c.data(this,\"events\").live);"
            + "for(p in t){j=t[p];if(j.live===a.type||j.altLive&&c.inArray(a.type,j.altLive)>-1){"
            + "i=j.data;i.beforeFilter&&i.beforeFilter[a.type]&&!i.beforeFilter[a.type](a)||f.push(j.selector);}"
            + "else delete t[p];}i=c(a.target).closest(f,a.currentTarget);n=0;for(l=i.length;n<l;n++)for(p in t){"
            + "j=t[p];o=i[n].elem;f=null;if(i[n].selector===j.selector){"
            + "if(j.live===\"mouseenter\"||j.live===\"mouseleave\")f=c(a.relatedTarget).closest(j.selector)[0];"
            + "if(!f||f!==o)d.push({elem:o,fn:j});}}n=0;for(l=d.length;n<l;n++){i=d[n];a.currentTarget=i.elem;"
            + "a.data=i.fn.data;if(i.fn.apply(i.elem,e)===false){b=false;break;}}return b;}"
            + "function ra(a,b){return[\"live\",a,b.replace(/\\./g,\"`\").replace(/ /g,\"&\")].join(\".\");}"
            + "function sa(a){return!a||!a.parentNode||a.parentNode.nodeType===11;}"
            + "function ta(a,b){var d=0;b.each(function(){if(this.nodeName===(a[d]&&a[d].nodeName)){"
            + "var f=c.data(a[d++]),e=c.data(this,f);if(f=f&&f.events){delete e.handle;e.events={};for(var i in f)"
            + "for(var j in f[i])c.event.add(this,i,f[i][j],f[i][j].data);}}});}"
            + "function ua(a,b,d){var f,e,i;if(a.length===1&&typeof a[0]===\"string\"&&a[0].length<512&&a[0]."
            + "indexOf(\"<option\")<0){e=true;if(i=c.fragments[a[0]])if(i!==1)f=i;}if(!f){b=b&&b[0]?"
            + "b[0].ownerDocument||b[0]:s;f=b.createDocumentFragment();c.clean(a,b,f,d);}"
            + "if(e)c.fragments[a[0]]=i?f:1;return{fragment:f,cacheable:e};}"
            + "function T(a){for(var b=0,d,f;(d=a[b])!=null;b++)if(!c.noData[d.nodeName.toLowerCase()]&&(f=d[H]))"
            + "delete c.cache[f];}"
            + "function L(a,b){var d={};c.each(va.concat.apply([],va.slice(0,b)),function(){d[this]=a;});return d;}"
            + "function wa(a){return\"scrollTo\"in a&&a.document?a:a.nodeType===9?a.defaultView||a.parentWindow:"
            + "false;} something: while(x=y){} do {} while(x==y);";

        final String beautified = new JavaScriptBeautifier().beautify(source);

        assertEquals(source.replaceAll("\\s", ""), beautified.replaceAll("\\s", ""));
    }

    /**
     * Tests loading minimized jQuery.
     * @throws IOException if an error occurs
     */
    @Test
    public void jquery() throws IOException {
        final String source = load("src/test/resources/minimized/jquery-1.4.min.js");
        new JavaScriptBeautifier().beautify(source);
    }

    /**
     * Tests loading minimized sample of GWT.
     * @throws IOException if an error occurs
     */
    @Test
    public void gwt() throws IOException {
        final String source = load("src/test/resources/minimized/GWT-sample-min.js");
        new JavaScriptBeautifier().beautify(source);
    }

    private String load(final String file) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }
}
