
// http://www.nabble.com/Mozilla---JavaScript-Debugger-f6669.html
// http://ginatrapani.org/spun/posts/2006/11/01/firefox-20-extension-development
// http://developer.mozilla.org/en/docs/Code_snippets
// http://code.google.com/p/fbug/source/browse/branches/firebug1.2/components/firebug-service.js

const FilePicker = Components.classes["@mozilla.org/filepicker;1"];
const FileOutputStream = Components.classes["@mozilla.org/network/file-output-stream;1"];
const ConverterOutputStream = Components.classes["@mozilla.org/intl/converter-output-stream;1"];
const PromptService = Components.classes["@mozilla.org/embedcomp/prompt-service;1"];
const ConsoleService = Components.classes["@mozilla.org/consoleservice;1"];
const DebuggerService = Components.classes["@mozilla.org/js/jsd/debugger-service;1"];

const nsIFilePicker = Components.interfaces.nsIFilePicker;
const nsIFileOutputStream = Components.interfaces.nsIFileOutputStream;
const nsIConverterOutputStream = Components.interfaces.nsIConverterOutputStream;
const nsIPromptService = Components.interfaces.nsIPromptService;
const nsIConsoleService  = Components.interfaces.nsIConsoleService ;
const jsdIDebuggerService = Components.interfaces.jsdIDebuggerService;
const jsdIExecutionHook = Components.interfaces.jsdIExecutionHook;
const jsdICallHook = Components.interfaces.jsdICallHook;
const jsdIProperty = Components.interfaces.jsdIProperty;
const jsdIValue = Components.interfaces.jsdIValue;

var ftLog = {
	_tab: new Array(),
	push: function(s)
	{
		this.length++;
		this._tab.push(s);
	},
	getLine: function(i)
	{
		return this._tab[i];
	},
	length: 0,
	clear: function()
	{
		this._tab = new Array();
		this.length = 0;
	}
};

var ftEnabled = false;
var ftLogNextInterruptAsFunctionCall = false; // Need to log function calls in the next interrupt, because the function hook doesn't provide enough info.
var ftSkipNextInterrupt = false; // Need to skip interrupts after functions return, because it's on the line which called the function, which we've already logged.

var firetrace = {

	// Performs initialization. Called when the extension is first loaded.
	onLoad: function() {
		this.initialized = true;
		this.strings = document.getElementById("firetrace-strings");
	},

	// Invoked when a new page is loaded.
	onPageLoad: function(event) {
		// Can't use "this.xxx" in this method... because it's used as an event handler?
		// http://developer.mozilla.org/en/docs/Code_snippets:Tabbed_browser#Detecting_page_load
		if(!ftEnabled) return;
		if (event.originalTarget instanceof HTMLDocument) {
			var doc = event.originalTarget;
			if (!doc.defaultView.frameElement) {
				// TODO: UNCOMMENT: hookDomCalls(doc);
				/* TODO: DELETE BELOW
				//msg('a');
				msg("1 "+ doc.body);
				msg("2 " + doc.body.wrappedJSObject);
				var body = doc.body.wrappedJSObject;
				//msg(body);
				body._appendChild = body.appendChild;
				//msg('c');
				body.appendChild = function() {
					msg("3 " + arguments[0]);
					msg("body.appendChild(" + arguments[0].innerHTML + ")");
					body._appendChild(arguments[0]);
				};
				//	function() { msg('abc'); }
				//msg('d');*/
			}
		}
	},

	// Triggers the javascript tracing on/off.
	trigger: function(e) {

		ftEnabled = !ftEnabled;
		var debuggerService = DebuggerService.getService(jsdIDebuggerService);
		var triggerButton = document.getElementById("firetrace-trigger-button");

		if(!ftEnabled) {
			if(triggerButton) triggerButton.style.listStyleImage = "url('chrome://firetrace/skin/trigger-button-start.png')";
			msg("Disabled tracing. " + ftLog.length + " calls traced");
			debuggerService.off();
			debuggerService.throwHook = null;
			debuggerService.functionHook = null;
			debuggerService.interruptHook = null;
			return;
		}

		if(triggerButton) triggerButton.style.listStyleImage = "url('chrome://firetrace/skin/trigger-button-stop.png')";
		msg("Enabled tracing.");

		// Function called when an exception is thrown (even if it's caught).
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIExecutionHook.html
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIStackFrame.html
		var throwHook = function(frame, type, val) {
			// Don't log this error if it's internal to Firefox.
			var file = frame.script.fileName;
			if(shouldExclude(file)) return jsdIExecutionHook.RETURN_CONTINUE_THROW;
			// Log the exception.
			var indent = getIndentationFor(frame);
			var line = frame.line;
			ftLog.push(indent + "Threw exception @ line " + line + " of " + file);
			// Allow the debugger to continue.
			return jsdIExecutionHook.RETURN_CONTINUE_THROW;
		}

		// Function called when a function is invoked or returns.
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdICallHook.html
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIStackFrame.html
		var functionHook = function(frame, type) {
			
			// Don't log this method call if it's internal to Firefox.
			var file = frame.script.fileName;
			if(shouldExclude(file)) return;
			
			if(type == jsdICallHook.TYPE_FUNCTION_RETURN) {
				// If the function is returning; don't log it as a method call.
				// Plus, skip the next interrupt log, since it will be the line that called the function,
				// and we've already logged it.
				ftSkipNextInterrupt = true;
				return;
			} else {
				// OK, we can use the next interrupt to log this function call.
				ftLogNextInterruptAsFunctionCall = true;
			}
		}

		// Function called at each increment of the PC.
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIExecutionHook.html
		// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIStackFrame.html
		var interruptHook = function(frame, type, val) {
			
			// If asked to skip this interrupt, do so.
			if(ftSkipNextInterrupt) {
				ftSkipNextInterrupt = false;
				return jsdIExecutionHook.RETURN_CONTINUE;
			}
			
			// Log a function call if necessary.
			if(ftLogNextInterruptAsFunctionCall) {
				ftLogNextInterruptAsFunctionCall = false;
				// Start with indentation.
				var indent = getIndentationFor(frame.callingFrame);
				// Continue with argument info.
				// http://websvn.wyzo.com/filedetails.php?repname=wyzo&path=%2Fmozilla%2Fextensions%2Fvenkman%2Fresources%2Fcontent%2Fvenkman-debugger.js&rev=1&sc=1
				var scope = frame.scope;
				var args = "";
				var p = new Object();
				frame.script.functionObject.getProperties(p, {});
				p = p.value;
				for(var i = p.length - 1; i >= 0; i--) {
					if(p[i].flags & jsdIProperty.FLAG_ARGUMENT) {
						var argName = p[i].name.stringValue;
						var argValue = formatValue(scope.getProperty(argName).value);
						args += argName + ": " + argValue + ", ";
					}
				}
				if(args.lastIndexOf(", ") == args.length - 2) {
					args = args.substring(0, args.length - 2);
				}
				// Continue with other information.
				var file = "unknown", line = "unknown";
				if (frame.callingFrame != null)
				{
					file = frame.callingFrame.script.fileName;
					line = frame.callingFrame.line;
				}
				var funcName = getFunctionName(frame);

				// Log the gathered information.
				var s = indent + funcName + "(" + args + ") @ line " + line + " of " + file;
				ftLog.push(s);
			}
			
			// Log the execution of this line of code if necessary.
			/* TODO: DELETE BELOW IF WE DON'T WANT TO LOG EACH LINE
			var file = frame.script.fileName;
			if(!shouldExclude(file) && !frame.isDebugger && !frame.isNative) {
				var indent = getIndentationFor(frame);
				var line = frame.line;
				var baseLine = frame.script.baseLineNumber;
				var actualLine = line - baseLine;
				var source = frame.script.functionSource;
				var lines = source.split('\n');
				var s = indent + trim(lines[actualLine]);
				// This method is often called multiple times per line of code, but we only want to log each line of code once.
				if(ftLog[ftLog.length-1] != s) {
					ftLog.push(s);
				}
			}
			*/
			
			// Allow execution to continue.
			return jsdIExecutionHook.RETURN_CONTINUE;
		}

		debuggerService.on();
		debuggerService.throwHook = { onExecute: throwHook };
		debuggerService.functionHook = { onCall: functionHook };
		debuggerService.interruptHook = { onExecute: interruptHook };

	},

	// Saves the javascript trace log to a file.
	saveLog: function(e) {
		// http://developer.mozilla.org/en/docs/nsIFilePicker
		// http://developer.mozilla.org/en/docs/Writing_textual_data
		// http://www.xulplanet.com/references/xpcomref/ifaces/nsIFileOutputStream.html
		// http://www.xulplanet.com/references/xpcomref/ifaces/nsIConverterOutputStream.html
		var fp = FilePicker.createInstance(nsIFilePicker);
		fp.init(window, "Save Trace", nsIFilePicker.modeSave);
		fp.appendFilters(nsIFilePicker.filterAll);
		var rv = fp.show();
		if (rv == nsIFilePicker.returnOK || rv == nsIFilePicker.returnReplace) {
			var file = fp.file;
			var fos = FileOutputStream.createInstance(nsIFileOutputStream);
			fos.init(file, -1, -1, -1);
			var cos = ConverterOutputStream.createInstance(nsIConverterOutputStream);
			cos.init(fos, "UTF-8", 4096, 0x0000);
			for(var i = 0; i < ftLog.length; i++) {
				cos.writeString(ftLog.getLine(i) + "\r\n");
			}
			cos.close();
		}
	},

	// Clears the javascript trace log.
	clearLog: function() {
		msg("Cleared " + ftLog.length + " log entries.");
		ftLog.clear();
	}

};

function trim(s) {
	return s.replace(/^\s+|\s+$/g, "");
}

function normalizeWhitespace(s) {
	return s.replace(/\s+/g, " ");
}

function msg(s) {
	var consoleService = ConsoleService.getService(nsIConsoleService);
	consoleService.logStringMessage(s);
}

function shouldExclude(fileName) {
	return (
		(fileName.indexOf("chrome://") == 0) || // Firefox Chrome
		(fileName.indexOf("file:/") == 0 && fileName.indexOf("/extensions/") != -1) || // Firefox Extension
		(fileName.indexOf("file:/") == 0 && fileName.indexOf("/components/") != -1) || // Firefox Component
		(fileName.indexOf("XStringBundle") == 0) // Random Firefox File
	);
}

// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIStackFrame.html
function getIndentationFor(frame) {
	var indent = "";
	if(frame) {
		var parent = frame.callingFrame;
		while(parent) {
			if(!parent.isDebugger && !parent.isNative) indent += "   ";
			parent = parent.callingFrame;
		}
	}
	return indent;
}

// http://www.xulplanet.com/references/xpcomref/ifaces/jsdIValue.html
function formatValue(value) {
	switch(value.jsType) {
		case jsdIValue.TYPE_FUNCTION:
			// Don't return the string value of the function, because it's usually
			// multiple lines of content and messes up the log.
			return "[function " + value.jsFunctionName + "]";
		default:
			// TODO: if it's an object, it may have properties which are functions; they should be formatted as above.
			// For now, we just normalize the whitespace so that the function doesn't span multiple lines and kill our formatting.
			return normalizeWhitespace(value.stringValue);
	}
}

function getFunctionName(frame)
{
	var name = frame.functionName
	if ("anonymous" == name)
	{
        // An anonymous function -- try to figure out how it was referenced.
        // For example, someone may have set foo.prototype.bar = function() { ... };
        // And then called fooInstance.bar() -- in which case it's "named" bar.
		var currentFunction = frame.script.functionObject.getWrappedValue();
		var currentThis = frame.thisValue.getWrappedValue()

		for (var i in currentThis)
		{
			if ((typeof i == "string") && currentThis[i] === currentFunction)
				return i;
		}
	}
	return name;
}

function hookDomCalls(o) {
	// Unwrap the object if necessary.
	// http://developer.mozilla.org/en/docs/XPCNativeWrapper#Protected_script_accessing_an_untrusted_object
	if(("" + o).indexOf("[object XPCNativeWrapper ") == 0) o = o.wrappedJSObject;
	// Skip some types of objects.
	// TODO: not skipping the right ones... still loading the Java plugin...
	var os = "" + o;
	if(os == "[object Plugin]" || os == "[object PluginArray]" || os == "[object MimeType]" || os == "[object MimeTypeArray]") {
		return;
	}
	// Flag this object to void infinite recursion later on.
	o._____hooked = true;
	// Hook all the functions in this object.
	for(var p in o) {
		if(typeof o[p] == "function") {
			// Skip some functions: window.Components(), document.defaultView().
			if(p == "Components" || p == "defaultView") continue;
			msg("hooking " + o + "." + p + "()");
			// Hook this function.
			try {
				var p2 = "_____" + p;
				o[p2] = o[p];
				o[p] = function() {
					try {
						var s = o + "." + p + "(";
						for(var i = 0; i < arguments.length; i++) {
							s += arguments[i] + ", ";
						}
						if(s.lastIndexOf(", ") == s.length - 2) {
							s = s.substring(0, s.length - 2);
						}
						s += ")";
						msg(s);
						/*for(var j = 0; j < arguments.length; j++) {
							if(("" + arguments[j]).indexOf("[object XPCNativeWrapper ") == 0) {
								arguments[j] = arguments[j].wrappedJSObject;
							}
						}*/
						o[p2].apply(o, arguments);
					} catch(e) {
						msg("Error executing function '" + p + "': " + e);
					}
				};
			} catch(e2) {
				msg("Error hooking function '" + p + "': " + e2);
			}
		} else if(typeof o[p] == "object") {
			// Recursively hook all the functions in all of the object's members (unless we can't / shouldn't).
			try {
				if(o[p]._____hooked) continue;
			}catch(e) {
				// This happens sometimes, not sure why.
				// TypeError: foo has no properties.
				continue;
			}
			ftLog.push("hooking: " + o + "." + p);
			hookDomCalls(o[p]);
		}
	}
}

window.addEventListener("load", function() { firetrace.onLoad(); }, false);
window.addEventListener("load", function() { gBrowser.addEventListener("load", firetrace.onPageLoad, true); }, false);
