/*
 * Copyright (c) 2001-2007, TIBCO Software Inc.
 * Use, modification, and distribution subject to terms of license.
 */
jsx3.require("jsx3.chart.Axis");jsx3.Class.defineClass("jsx3.chart.LinearAxis",jsx3.chart.Axis,null,function(n,d){n.MIN_INTERVALS=5;n.MAX_INTERVALS=11;n.Tf=200;n.Qs=1.1;n.Qc=0;n.fj=100;n.pm=20;d.init=function(e,c,r){this.jsxsuper(e,c,r);this.autoAdjust=jsx3.Boolean.TRUE;this.baseAtZero=jsx3.Boolean.TRUE;this.min=null;this.max=null;this.interval=null;this.xI("a_",n.Qc);this.xI("kU",n.fj);this.xI("G4",n.pm);};d.getAutoAdjust=function(){return this.autoAdjust;};d.setAutoAdjust=function(j){this.autoAdjust=j;};d.getBaseAtZero=function(){return this.baseAtZero;};d.setBaseAtZero=function(i){this.baseAtZero=i;};d.getMin=function(){return this.min;};d.setMin=function(q){this.min=q;};d.getMax=function(){return this.max;};d.setMax=function(k){this.max=k;};d.getInterval=function(){return this.interval;};d.setInterval=function(c){this.interval=c;};d.g6=function(){var ob=false;if(this.autoAdjust)ob=this.Pd();if(!ob){this.xI("a_",this.min!=null?this.min:n.Qc);this.xI("kU",this.max!=null?this.max:n.fj);this.xI("G4",this.interval!=null?this.interval:n.pm);}};d.Pd=function(){var qb=this.getChart();if(qb==null)return false;var Ic=qb.getRangeForAxis(this);var bc,Ab;if(Ic==null){jsx3.chart.LOG.debug("no range for axis "+this+" in chart "+qb);if(this.min!=null||this.max!=null){bc=this.min||n.Qc;Ab=this.max||bc+n.fj;}else{return false;}}else{bc=Ic[0];Ab=Ic[1];}var ic=null,B=null,R=null;if(this.min!=null)ic=this.min;else{if(bc>=0&&this.baseAtZero)ic=0;}if(this.max!=null)B=this.max;else{if(Ab<=0&&this.baseAtZero)B=0;}R=this.interval;if(R==null){var Ub=1;var N=null,Nc=null;if(ic!=null){N=ic;}else{N=bc;Ub=Ub*n.Qs;}if(B!=null){Nc=B;}else{Nc=Ab;Ub=Ub*n.Qs;}var Cb=Nc-N;var tc=Cb*Ub;R=1;if(tc>0){tc=tc/n.MIN_INTERVALS;while(tc<1){R=R/10;tc=tc*10;}while(tc>10){R=R*10;tc=tc/10;}if(tc>5){R=R*5;}else{if(tc>2){R=R*2;}}}}if(ic==null){var Q=bc-(n.Qs-1)*(Ab-bc)/2;ic=R*Math.floor(Q/R);if(B!=null){ic=ic-B%R;}}if(B==null){var S=Ab+(n.Qs-1)*(Ab-bc)/2;B=R*Math.ceil(S/R);if(ic!=null){B=B+ic%R;}}this.xI("a_",ic);this.xI("kU",B);this.xI("G4",R);return true;};d.a1=function(h){return this.Q0("a_")+h*this.Q0("G4");};d.RQ=function(){var Mb=this.Q0("kU");var Yb=this.Q0("a_");var W=this.Q0("G4");var Kb=[];var yc=Yb;while(yc<=Mb&&Kb.length<n.Tf){Kb.push(this.getCoordinateFor(yc));yc=yc+W;}return Kb;};d.vX=function(){return this.Q0("a_")<0&&this.Q0("kU")>0;};d.getCoordinateFor=function(m){var Lc=this.Q0("kU");var Yb=this.Q0("a_");if(m<Yb)return this.horizontal?0:this.length;if(m>Lc)return this.horizontal?this.length:0;var Ac=Math.round(this.length*(m-Yb)/(Lc-Yb));return this.horizontal?Ac:this.length-Ac;};d.getCoordinateForNoClip=function(a){var N=this.Q0("kU");var jb=this.Q0("a_");var ab=Math.round(a*1000)/1000;var Cc=this.length*((ab-jb)/(N-jb));return Math.round(this.horizontal?Cc:this.length-Cc);};d.toString=function(){return "[LinearAxis '"+this.getName()+"' hor:"+this.horizontal+" pri:"+this.primary+"]";};n.getVersion=function(){return jsx3.chart.q2;};});
