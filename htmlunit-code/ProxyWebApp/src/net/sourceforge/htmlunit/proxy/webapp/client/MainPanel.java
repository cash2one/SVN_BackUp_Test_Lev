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
package net.sourceforge.htmlunit.proxy.webapp.client;

import net.sourceforge.htmlunit.proxy.webapp.shared.LogEntry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * The main panel.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5525 $
 */
public class MainPanel extends Composite {

    interface Binder extends UiBinder<Widget, MainPanel> { }
    private static final Binder binder_ = GWT.create(Binder.class);

    @UiField CheckBox timeCheckBox_;
    @UiField Button clearButton_;
    @UiField TextArea logTextArea_;

    private final LogServiceAsync logService_ = GWT.create(LogService.class);

    private int counter_;
    private boolean isError_;

    private LogEntry[] entries_ = new LogEntry[0];

    /**
     * Constructor.
     */
    public MainPanel() {
        initWidget(binder_.createAndBindUi(this));

        final Timer timer = new Timer() {

            @Override
            public void run() {
                if (!isError_) {
                    logService_.getLog(counter_, new AsyncCallback<LogEntry[]>() {

                        public void onSuccess(final LogEntry[] logs) {
                            if (logs.length != 0) {
                                final LogEntry[] newEntries = new LogEntry[entries_.length + logs.length];
                                System.arraycopy(entries_, 0, newEntries, 0, entries_.length);
                                System.arraycopy(logs, 0, newEntries, entries_.length, logs.length);
                                entries_ = newEntries;
                                counter_ += logs.length;
                                updateTextArea();
                            }
                        }

                        public void onFailure(final Throwable caught) {
                            isError_ = true;
                            Window.alert("Failure connecting to server " + caught);
                            isError_ = false;
                        }
                    });
                }
            }
        };
        timer.scheduleRepeating(1000);
    }

    /**
     * A handler.
     * @param e the value change event
     */
    @UiHandler("timeCheckBox_")
    protected void doChangeValue(final ValueChangeEvent<Boolean> e) {
        updateTextArea();
    }

    /**
     * A handler.
     * @param event the click event
     */
    @UiHandler("clearButton_")
    public void onClick(final ClickEvent event) {
        entries_ = new LogEntry[0];
        updateTextArea();
    }

    private void updateTextArea() {
        String data = "";
        for (final LogEntry log : entries_) {
            data += (timeCheckBox_.getValue() ? (log.getTime() + " : ") : "") + log.getValue() + '\n';
        }
        logTextArea_.setText(data);
        logTextArea_.setCursorPos(data.length());
    }
}
