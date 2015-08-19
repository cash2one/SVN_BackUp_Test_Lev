/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.html;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLIMAGE_BLANK_SRC_AS_EMPTY;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLIMAGE_HTMLELEMENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLIMAGE_HTMLUNKNOWNELEMENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLIMAGE_INVISIBLE_NO_SRC;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_IMAGE_COMPLETE_RETURNS_TRUE_FOR_NO_REQUEST;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.javascript.PostponedAction;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Node;
import com.gargoylesoftware.htmlunit.javascript.host.event.Event;

/**
 * Wrapper for the HTML element "img".
 *
 * @version $Revision: 10803 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author David K. Taylor
 * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
 * @author Ahmed Ashour
 * @author <a href="mailto:knut.johannes.dahle@gmail.com">Knut Johannes Dahle</a>
 * @author Ronald Brill
 * @author Frank Danek
 * @author Carsten Steul
 */
public class HtmlImage extends HtmlElement {

    private static final Log LOG = LogFactory.getLog(HtmlImage.class);

    /** The HTML tag represented by this element. */
    public static final String TAG_NAME = "img";
    /** Another HTML tag represented by this element. */
    public static final String TAG_NAME2 = "image";

    private final String originalQualifiedName_;

    private int lastClickX_;
    private int lastClickY_;
    private WebResponse imageWebResponse_;
    private ImageData imageData_;
    private boolean downloaded_;
    private boolean onloadInvoked_;
    private boolean createdByJavascript_;

    /**
     * Creates a new instance.
     *
     * @param qualifiedName the qualified name of the element type to instantiate
     * @param page the page that contains this element
     * @param attributes the initial attributes
     */
    HtmlImage(final String qualifiedName, final SgmlPage page,
            final Map<String, DomAttr> attributes) {
        super(unifyLocalName(qualifiedName), page, attributes);
        originalQualifiedName_ = qualifiedName;
    }

    private static String unifyLocalName(final String qualifiedName) {
        if (qualifiedName != null && qualifiedName.endsWith(TAG_NAME2)) {
            final int pos = qualifiedName.lastIndexOf(TAG_NAME2);
            return qualifiedName.substring(0, pos) + TAG_NAME;
        }
        return qualifiedName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAddedToPage() {
        doOnLoad();
        super.onAddedToPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeNS(final String namespaceURI, final String qualifiedName, final String value) {
        final HtmlPage htmlPage = getHtmlPageOrNull();
        if ("src".equals(qualifiedName) && value != ATTRIBUTE_NOT_DEFINED
                && htmlPage != null) {
            final String oldValue = getAttributeNS(namespaceURI, qualifiedName);
            if (!oldValue.equals(value)) {
                super.setAttributeNS(namespaceURI, qualifiedName, value);

                // onload handlers may need to be invoked again, and a new image may need to be downloaded
                onloadInvoked_ = false;
                downloaded_ = false;

                final String readyState = htmlPage.getReadyState();
                if (READY_STATE_LOADING.equals(readyState)) {
                    final PostponedAction action = new PostponedAction(getPage()) {
                        @Override
                        public void execute() throws Exception {
                            doOnLoad();
                        }
                    };
                    htmlPage.addAfterLoadAction(action);
                    return;
                }
                doOnLoad();
                return;
            }
        }

        super.setAttributeNS(namespaceURI, qualifiedName, value);
    }

    /**
     * <p><span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span></p>
     *
     * <p>Executes this element's <tt>onload</tt> handler if it has one. This method also downloads the image
     * if this element has an <tt>onload</tt> handler (prior to invoking said handler), because applications
     * sometimes use images to send information to the server and use the <tt>onload</tt> handler to get notified
     * when the information has been received by the server.</p>
     *
     * <p>See <a href="http://www.nabble.com/How-should-we-handle-image.onload--tt9850876.html">here</a> and
     * <a href="http://www.nabble.com/Image-Onload-Support-td18895781.html">here</a> for the discussion which
     * lead up to this method.</p>
     *
     * <p>This method may be called multiple times, but will only attempt to execute the <tt>onload</tt>
     * handler the first time it is invoked.</p>
     */
    public void doOnLoad() {
        if (onloadInvoked_) {
            return;
        }

        final HtmlPage htmlPage = getHtmlPageOrNull();
        if (htmlPage == null) {
            return; // nothing to do if embedded in XML code
        }

        final WebClient client = htmlPage.getWebClient();
        if (!client.getOptions().isJavaScriptEnabled()) {
            onloadInvoked_ = true;
            return;
        }

        if (hasEventHandlers("onload") && !getSrcAttribute().isEmpty()) {
            onloadInvoked_ = true;
            // An onload handler and source are defined; we need to download the image and then call the onload handler.
            boolean ok;
            try {
                downloadImageIfNeeded();
                final int i = imageWebResponse_.getStatusCode();
                ok = (i >= HttpStatus.SC_OK && i < HttpStatus.SC_MULTIPLE_CHOICES) || i == HttpStatus.SC_USE_PROXY;
            }
            catch (final IOException e) {
                ok = false;
            }
            // If the download was a success, trigger the onload handler.
            if (ok) {
                final Event event = new Event(this, Event.TYPE_LOAD);
                final Node scriptObject = (Node) getScriptObject();

                final String readyState = htmlPage.getReadyState();
                if (READY_STATE_LOADING.equals(readyState)) {
                    final PostponedAction action = new PostponedAction(getPage()) {
                        @Override
                        public void execute() throws Exception {
                            scriptObject.executeEvent(event);
                        }
                    };
                    htmlPage.addAfterLoadAction(action);
                }
                else {
                    scriptObject.executeEvent(event);
                }
            }
            else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to download image for tag " + this + "; not firing onload event.");
                }
            }
        }
    }

    /**
     * Returns the value of the attribute "src". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "src" or an empty string if that attribute isn't defined
     */
    public final String getSrcAttribute() {
        return getSrcAttributeNormalized();
    }

    /**
     * Returns the value of the attribute "alt". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "alt" or an empty string if that attribute isn't defined
     */
    public final String getAltAttribute() {
        return getAttribute("alt");
    }

    /**
     * Returns the value of the attribute "name". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "name" or an empty string if that attribute isn't defined
     */
    public final String getNameAttribute() {
        return getAttribute("name");
    }

    /**
     * Returns the value of the attribute "longdesc". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "longdesc" or an empty string if that attribute isn't defined
     */
    public final String getLongDescAttribute() {
        return getAttribute("longdesc");
    }

    /**
     * Returns the value of the attribute "height". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "height" or an empty string if that attribute isn't defined
     */
    public final String getHeightAttribute() {
        return getAttribute("height");
    }

    /**
     * Returns the value of the attribute "width". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "width" or an empty string if that attribute isn't defined
     */
    public final String getWidthAttribute() {
        return getAttribute("width");
    }

    /**
     * Returns the value of the attribute "usemap". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "usemap" or an empty string if that attribute isn't defined
     */
    public final String getUseMapAttribute() {
        return getAttribute("usemap");
    }

    /**
     * Returns the value of the attribute "ismap". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "ismap" or an empty string if that attribute isn't defined
     */
    public final String getIsmapAttribute() {
        return getAttribute("ismap");
    }

    /**
     * Returns the value of the attribute "align". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "align" or an empty string if that attribute isn't defined
     */
    public final String getAlignAttribute() {
        return getAttribute("align");
    }

    /**
     * Returns the value of the attribute "border". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "border" or an empty string if that attribute isn't defined
     */
    public final String getBorderAttribute() {
        return getAttribute("border");
    }

    /**
     * Returns the value of the attribute "hspace". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "hspace" or an empty string if that attribute isn't defined
     */
    public final String getHspaceAttribute() {
        return getAttribute("hspace");
    }

    /**
     * Returns the value of the attribute "vspace". Refer to the
     * <a href='http://www.w3.org/TR/html401/'>HTML 4.01</a>
     * documentation for details on the use of this attribute.
     *
     * @return the value of the attribute "vspace" or an empty string if that attribute isn't defined
     */
    public final String getVspaceAttribute() {
        return getAttribute("vspace");
    }

    /**
     * <p>Returns the image's actual height (<b>not</b> the image's {@link #getHeightAttribute() height attribute}).</p>
     * <p><span style="color:red">POTENTIAL PERFORMANCE KILLER - DOWNLOADS THE IMAGE - USE AT YOUR OWN RISK</span></p>
     * <p>If the image has not already been downloaded, this method triggers a download and caches the image.</p>
     *
     * @return the image's actual height
     * @throws IOException if an error occurs while downloading or reading the image
     */
    public int getHeight() throws IOException {
        return getImageReader().getHeight(0);
    }

    /**
     * <p>Returns the image's actual width (<b>not</b> the image's {@link #getWidthAttribute() width attribute}).</p>
     * <p><span style="color:red">POTENTIAL PERFORMANCE KILLER - DOWNLOADS THE IMAGE - USE AT YOUR OWN RISK</span></p>
     * <p>If the image has not already been downloaded, this method triggers a download and caches the image.</p>
     *
     * @return the image's actual width
     * @throws IOException if an error occurs while downloading or reading the image
     */
    public int getWidth() throws IOException {
        return getImageReader().getWidth(0);
    }

    /**
     * <p>Returns the <tt>ImageReader</tt> which can be used to read the image contained by this image element.</p>
     * <p><span style="color:red">POTENTIAL PERFORMANCE KILLER - DOWNLOADS THE IMAGE - USE AT YOUR OWN RISK</span></p>
     * <p>If the image has not already been downloaded, this method triggers a download and caches the image.</p>
     *
     * @return the <tt>ImageReader</tt> which can be used to read the image contained by this image element
     * @throws IOException if an error occurs while downloading or reading the image
     */
    public ImageReader getImageReader() throws IOException {
        readImageIfNeeded();
        return imageData_.getImageReader();
    }

    /**
     * <p>Returns the <tt>WebResponse</tt> for the image contained by this image element.</p>
     * <p><span style="color:red">POTENTIAL PERFORMANCE KILLER - DOWNLOADS THE IMAGE - USE AT YOUR OWN RISK</span></p>
     * <p>If the image has not already been downloaded and <tt>downloadIfNeeded</tt> is <tt>true</tt>, this method
     * triggers a download and caches the image.</p>
     *
     * @param downloadIfNeeded whether or not the image should be downloaded (if it hasn't already been downloaded)
     * @return <tt>null</tt> if no download should be performed and one hasn't already been triggered; otherwise,
     *         the response received when performing a request for the image referenced by this element
     * @throws IOException if an error occurs while downloading the image
     */
    public WebResponse getWebResponse(final boolean downloadIfNeeded) throws IOException {
        if (downloadIfNeeded) {
            downloadImageIfNeeded();
        }
        return imageWebResponse_;
    }

    /**
     * <p>Downloads the image contained by this image element.</p>
     * <p><span style="color:red">POTENTIAL PERFORMANCE KILLER - DOWNLOADS THE IMAGE - USE AT YOUR OWN RISK</span></p>
     * <p>If the image has not already been downloaded, this method triggers a download and caches the image.</p>
     *
     * @throws IOException if an error occurs while downloading the image
     */
    private void downloadImageIfNeeded() throws IOException {
        if (!downloaded_) {
            // HTMLIMAGE_BLANK_SRC_AS_EMPTY
            final String src = getSrcAttribute();
            if (!"".equals(src)
                    && !(hasFeature(HTMLIMAGE_BLANK_SRC_AS_EMPTY) && StringUtils.isBlank(src))) {
                final HtmlPage page = (HtmlPage) getPage();
                final WebClient webclient = page.getWebClient();

                final URL url = page.getFullyQualifiedUrl(src);
                final String accept = webclient.getBrowserVersion().getImgAcceptHeader();
                final WebRequest request = new WebRequest(url, accept);
                request.setAdditionalHeader("Referer", page.getUrl().toExternalForm());
                imageWebResponse_ = webclient.loadWebResponse(request);
            }
            imageData_ = null;
            downloaded_ = true;
        }
    }

    private void readImageIfNeeded() throws IOException {
        downloadImageIfNeeded();
        if (imageData_ == null) {
            if (null == imageWebResponse_) {
                throw new IOException("No image response available");
            }
            final ImageInputStream iis = ImageIO.createImageInputStream(imageWebResponse_.getContentAsStream());
            final Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                iis.close();
                throw new IOException("No image detected in response");
            }
            final ImageReader imageReader = iter.next();
            imageReader.setInput(iis);
            imageData_ = new ImageData(imageReader);

            // dispose all others
            while (iter.hasNext()) {
                iter.next().dispose();
            }
        }
    }

    /**
     * Simulates clicking this element at the specified position. This only makes sense for
     * an image map (currently only server side), where the position matters. This method
     * returns the page contained by this image's window after the click, which may or may not
     * be the same as the original page, depending on JavaScript event handlers, etc.
     *
     * @param x the x position of the click
     * @param y the y position of the click
     * @return the page contained by this image's window after the click
     * @exception IOException if an IO error occurs
     */
    public Page click(final int x, final int y) throws IOException {
        lastClickX_ = x;
        lastClickY_ = y;
        return super.click();
    }

    /**
     * Simulates clicking this element at the position <tt>(0, 0)</tt>. This method returns
     * the page contained by this image's window after the click, which may or may not be the
     * same as the original page, depending on JavaScript event handlers, etc.
     *
     * @return the page contained by this image's window after the click
     * @exception IOException if an IO error occurs
     */
    @Override
    @SuppressWarnings("unchecked")
    public Page click() throws IOException {
        return click(0, 0);
    }

    /**
     * Performs the click action on the enclosing A tag (if any).
     * {@inheritDoc}
     * @throws IOException if an IO error occurred
     */
    @Override
    protected boolean doClickStateUpdate() throws IOException {
        if (getUseMapAttribute() != ATTRIBUTE_NOT_DEFINED) {
            // remove initial '#'
            final String mapName = getUseMapAttribute().substring(1);
            final HtmlElement doc = ((HtmlPage) getPage()).getDocumentElement();
            final HtmlMap map = doc.getOneHtmlElementByAttribute("map", "name", mapName);
            for (final DomElement element : map.getChildElements()) {
                if (element instanceof HtmlArea) {
                    final HtmlArea area = (HtmlArea) element;
                    if (area.containsPoint(lastClickX_, lastClickY_)) {
                        area.doClickStateUpdate();
                        return false;
                    }
                }
            }
        }
        final HtmlAnchor anchor = (HtmlAnchor) getEnclosingElement("a");
        if (anchor == null) {
            return false;
        }
        if (getIsmapAttribute() != ATTRIBUTE_NOT_DEFINED) {
            final String suffix = "?" + lastClickX_ + "," + lastClickY_;
            anchor.doClickStateUpdate(suffix);
            return false;
        }
        anchor.doClickStateUpdate();
        return false;
    }

    /**
     * Saves this image as the specified file.
     * @param file the file to save to
     * @throws IOException if an IO error occurs
     */
    public void saveAs(final File file) throws IOException {
        final ImageReader reader = getImageReader();
        ImageIO.write(reader.read(0), reader.getFormatName(), file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DisplayStyle getDefaultStyleDisplay() {
        return DisplayStyle.INLINE;
    }

    /**
     * Wraps the ImageReader for an HtmlImage. This is necessary because an object with a finalize()
     * method is only garbage collected after the method has been run. Which causes all referenced
     * objects to also not be garbage collected until this happens. Because a HtmlImage references a lot
     * of objects which could all be garbage collected without impacting the ImageReader it is better to
     * wrap it in another class.
     */
    private static final class ImageData {

        private final ImageReader imageReader_;

        public ImageData(final ImageReader imageReader) {
            imageReader_ = imageReader;
        }

        public ImageReader getImageReader() {
            return imageReader_;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void finalize() throws Throwable {
            if (imageReader_ != null) {
                try {
                    final ImageInputStream stream = (ImageInputStream) imageReader_.getInput();
                    if (stream != null) {
                        stream.close();
                    }
                }
                catch (final IOException e) {
                    LOG.error(e.getMessage(), e);
                }
                finally {
                    imageReader_.setInput(null);
                    imageReader_.dispose();
                }
            }
            super.finalize();
        }
    }

    /**
     * @return true if the image was successfully downloaded
     */
    public boolean getComplete() {
        if (hasFeature(JS_IMAGE_COMPLETE_RETURNS_TRUE_FOR_NO_REQUEST)) {
            return downloaded_ || ATTRIBUTE_NOT_DEFINED == getSrcAttribute();
        }
        return imageData_ != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisplayed() {
        final String src = getSrcAttribute();
        if (hasFeature(HTMLIMAGE_INVISIBLE_NO_SRC)
                && (ATTRIBUTE_NOT_DEFINED == src
                    || (hasFeature(HTMLIMAGE_BLANK_SRC_AS_EMPTY) && StringUtils.isBlank(src)))) {
            return false;
        }
        return super.isDisplayed();
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Marks this frame as created by javascript. This is needed to handle
     * some special IE behavior.
     */
    public void markAsCreatedByJavascript() {
        createdByJavascript_ = true;
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Returns true if this frame was created by javascript. This is needed to handle
     * some special IE behavior.
     * @return true or false
     */
    public boolean wasCreatedByJavascript() {
        return createdByJavascript_;
    }

    /**
     * Returns the original element qualified name,
     * this is needed to differentiate between <tt>img</tt> and <tt>image</tt>.
     * @return the original element qualified name
     */
    public String getOriginalQualifiedName() {
        return originalQualifiedName_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalName() {
        if (wasCreatedByJavascript()
                && (hasFeature(HTMLIMAGE_HTMLELEMENT) || hasFeature(HTMLIMAGE_HTMLUNKNOWNELEMENT))) {
            return originalQualifiedName_;
        }
        return super.getLocalName();
    }
}
