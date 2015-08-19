/*
 * Copyright (c) 2001-2007, TIBCO Software Inc.
 * Use, modification, and distribution subject to terms of license.
 */
jsx3.Class.defineClass("jsx3.gui.HotKey",null,[jsx3.util.EventDispatcher],function(p,o){var Eb=jsx3.gui.Event;p.WAS_INVOKED="invoked";o.Ue=null;o.Zq=null;o.Aw=false;o.Iz=false;o.Ku=false;o.ty=false;o.Rc=true;o.Vx=false;p.valueOf=function(r,d){var I=r.toLowerCase().split("+");var gc=I.pop();var A=I.indexOf("ctrl")>=0;var Hc=I.indexOf("shift")>=0;var yb=I.indexOf("alt")>=0;var _=I.indexOf("meta")>=0;if(typeof(gc)=="string"&&gc.match(/^\[(\d+)\]$/))gc=parseInt(RegExp.$1);return new p(d||new Function(""),gc,Hc,A,yb,_);};o.init=function(i,h,b,d,a,c){if(!(typeof(i)=="function"))throw new jsx3.IllegalArgumentException("callback",i);this.Ue=i;this.Aw=b==null?null:Boolean(b);this.Iz=d==null?null:Boolean(d);this.ty=a==null?null:Boolean(a);this.Ku=c==null?null:Boolean(c);this.Zq=typeof(h)=="number"?h:p.keyDownCharToCode(h);if(this.Zq==null)throw new jsx3.IllegalArgumentException("key",h);};o.getKey=function(){var ab="";if(this.Ku)ab=ab+"meta+";if(this.ty)ab=ab+"alt+";if(this.Iz)ab=ab+"ctrl+";if(this.Aw)ab=ab+"shift+";var G=p.keyDownCodeToChar(this.Zq);ab=ab+(G!=null?G:"["+this.Zq+"]");return ab;};o.getKeyCode=function(){return this.Zq;};o.isMatch=function(f){var vc=f.keyCode()==this.Zq&&(this.Aw==null||f.shiftKey()==this.Aw)&&(this.Iz==null||f.ctrlKey()==this.Iz)&&(this.Ku==null||f.metaKey()==this.Ku)&&(this.ty==null||f.altKey()==this.ty);return vc;};o.invoke=function(s,r){if(this.Vx||!this.Rc)throw new jsx3.Exception("HotKey destroyed or not enabled");this.Ue.apply(s,r);this.publish({subject:p.WAS_INVOKED});};o.isEnabled=function(){return this.Rc;};o.setEnabled=function(a){this.Rc=a;};o.isDestroyed=function(){return this.Vx;};o.destroy=function(){this.Vx=true;delete this.Ue;};o.getFormatted=function(){var _b=null,Fb=null;if(jsx3.app.Browser.macosx){if(p.Tm.shift==null){var Ic=jsx3.html.getMode()==jsx3.html.MODE_FF_STRICT;p.Tm.shift=[Ic?"\u21EA":"\u0005",Eb.KEY_SHIFT];}_b="";Fb=p.Tm;}else{_b="+";Fb=p.Hu;}var H="";if(this.Iz)H=H+(Fb.ctrl[0]+_b);if(this.ty)H=H+(Fb.alt[0]+_b);if(this.Aw)H=H+(Fb.shift[0]+_b);if(this.Ku)H=H+(Fb.meta[0]+_b);var Dc=p.keyDownCodeToChar(this.Zq,true);H=H+(Dc!=null?Dc.length==1?Dc.toUpperCase():Dc:"["+this.Zq+"]");return H;};p.Tm={meta:["\u2318",Eb.KEY_META],alt:["\u2325",Eb.KEY_ALT],ctrl:["\u2303",Eb.KEY_CONTROL],shift:null,enter:["\u21A9",Eb.KEY_ENTER],esc:["\u238B",Eb.KEY_ESCAPE],tab:["\u21E5",Eb.KEY_TAB],del:["\u2326",Eb.KEY_DELETE],space:["\u2423",Eb.KEY_SPACE],backspace:["\u232B",Eb.KEY_BACKSPACE],up:["\u2191",Eb.KEY_ARROW_UP],down:["\u2193",Eb.KEY_ARROW_DOWN],left:["\u2190",Eb.KEY_ARROW_LEFT],right:["\u2192",Eb.KEY_ARROW_RIGHT],insert:["Insert",Eb.KEY_INSERT],home:["\u2196",Eb.KEY_HOME],end:["\u2198",Eb.KEY_END],pgup:["\u21DE",Eb.KEY_PAGE_UP],pgdn:["\u21DF",Eb.KEY_PAGE_DOWN]};p.Hu={meta:["Meta",Eb.KEY_META],alt:["Alt",Eb.KEY_ALT],ctrl:["Ctrl",Eb.KEY_CONTROL],shift:["Shift",Eb.KEY_SHIFT],enter:["Enter",Eb.KEY_ENTER],esc:["Esc",Eb.KEY_ESCAPE],tab:["Tab",Eb.KEY_TAB],del:["Del",Eb.KEY_DELETE],space:["Space",Eb.KEY_SPACE],backspace:["Backspace",Eb.KEY_BACKSPACE],up:["Up",Eb.KEY_ARROW_UP],down:["Down",Eb.KEY_ARROW_DOWN],left:["Left",Eb.KEY_ARROW_LEFT],right:["Right",Eb.KEY_ARROW_RIGHT],insert:["Insert",Eb.KEY_INSERT],home:["Home",Eb.KEY_HOME],end:["End",Eb.KEY_END],pgup:["PgUp",Eb.KEY_PAGE_UP],pgdn:["PgDn",Eb.KEY_PAGE_DOWN]};o.toString=function(){return "@HotKey key:"+this.Zq+" shift:"+this.Aw+" ctrl:"+this.Iz+" alt:"+this.ty+" meta:"+this.Ku;};p.Fx={39:222,44:188,45:189,46:190,47:191,59:186,61:187,91:219,92:220,93:221,96:192};p.keyDownCharToCode=function(n){var gc=null;if(n.length==1){var Fb=n.charCodeAt(0);if(Fb>=65&&Fb<=90)gc=Fb;else{if(Fb>=97&&Fb<=122)gc=Fb-32;else{if(Fb>=48&&Fb<=57)gc=Fb;else gc=p.Fx[Fb];}}}else{if(p.Hu[n.toLowerCase()]){gc=p.Hu[n.toLowerCase()][1];}else{if(n.match(/^[fF](\d\d?)$/)){gc=parseInt(RegExp.$1)+Eb.KEY_F1-1;}}}return gc;};p.keyDownCodeToChar=function(i,a){var Y=null;if(i>=65&&i<=90)Y=String.fromCharCode(i+97-65);else{if(i>=48&&i<=57)Y=String.fromCharCode(i);else{if(i>=Eb.KEY_F1&&i<=Eb.KEY_F15)Y="F"+(i-Eb.KEY_F1+1);else{for(var wb in p.Fx){if(p.Fx[wb]==i){Y=String.fromCharCode(wb);break;}}if(Y==null){var S=a&&jsx3.app.Browser.macosx?p.Tm:p.Hu;for(var wb in S){if(S[wb][1]==i){Y=S[wb][0];break;}}}}}}return Y;};});
