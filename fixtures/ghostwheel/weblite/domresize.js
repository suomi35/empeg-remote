/**************************************************
 * domresize.js
 * 24.07.2003
 * Alexander Graf <agraf@rz-online.net>
 *
 * based on
 *
 * dom-drag.js
 * 25.09.2001
 * www.youngpup.net
 **************************************************/

var Resize = {

	obj : null,

	cursors : new Array('move',     'w-resize',  'e-resize',  null,
	                    'n-resize', 'nw-resize', 'ne-resize', null,
	                    's-resize', 'sw-resize', 'se-resize'),

	init : function(o, oRoot, minX, maxX, minY, maxY, noDrag, resizeX, resizeY, keepX, keepY, fMapper, doCursor, canShift, canAlt, canCtrl)
	{
		o.root = oRoot && oRoot != null ? oRoot : o ;

		o.minX    = typeof minX    != 'undefined' ? minX    : null;
		o.maxX    = typeof maxX    != 'undefined' ? maxX    : null;
		o.minY    = typeof minY    != 'undefined' ? minY    : null;
		o.maxY    = typeof maxY    != 'undefined' ? maxY    : null;

		o.resizeX = typeof resizeX != 'undefined' ? resizeX : null;
		o.resizeY = typeof resizeY != 'undefined' ? resizeY : null;
		o.keepX   = typeof keepX   != 'undefined' ? keepX   : 1;
		o.keepY   = typeof keepY   != 'undefined' ? keepY   : 1;

		o.noDrag  = noDrag  ? true    : false;
		o.fMapper = fMapper ? fMapper : null ;

		o.doCur   = typeof doCursor != 'undefined' ? (doCursor ? true : false) : true;

		o.doAlt   = canAlt   ? true : false;
		o.doCtrl  = canCtrl  ? true : false;
		o.doShift = canShift ? true : false;
		o.doMod   = o.doAlt || o.doCtrl || o.doShift;

		if (isNaN(parseInt(o.root.style.left  )))
			o.root.style.left = '0px';
		if (isNaN(parseInt(o.root.style.top   )))
			o.root.style.top  = '0px';
		if (isNaN(parseInt(o.root.style.width )))
			o.root.style.width  = (isNaN(parseInt(o.root.width )) ? keepX : o.root.width ) + 'px';
		if (isNaN(parseInt(o.root.style.height)))
			o.root.style.height = (isNaN(parseInt(o.root.height)) ? keepY : o.root.height) + 'px';

		o.root.onDragStart   = new Function();
		o.root.onDragEnd     = new Function();
		o.root.onDrag        = new Function();
		o.root.onResizeStart = new Function();
		o.root.onResizeEnd   = new Function();
		o.root.onResize      = new Function();

		o.enable  = Resize.enable;
		o.disable = Resize.disable;
		o.oCursor = o.style.cursor;

		o.enable();
	},

	update : function(e)
	{
		e = Resize.fixE(e);

		var w  = parseInt(this.style.width );
		var h  = parseInt(this.style.height);
		var rx = (w<(this.resizeX*2)) ? Math.max(Math.floor(w/2), 1) : this.resizeX;
		var ry = (h<(this.resizeY*2)) ? Math.max(Math.floor(h/2), 1) : this.resizeY;

		this.rMode = (rx ? ((e.layerX < rx) ? 1 : (e.layerX >= w - rx) ? 2 : 0) : 0) +
		             (ry ? ((e.layerY < ry) ? 4 : (e.layerY >= h - ry) ? 8 : 0) : 0)

		if (this.doCur) this.style.cursor = (!this.rMode && (this.noDrag || (this.maxX-this.minX == w && this.maxY-this.minY == h))) ? this.oCursor : Resize.cursors[this.rMode];

		return false;
	},

	start : function(e)
	{
		var o = this;
		if (o.rMode || !o.noDrag){
			e = Resize.fixE(e);

			var ex = e.clientX;
			var ey = e.clientY;
			var x  = parseInt(o.root.style.left  );
			var y  = parseInt(o.root.style.top   );
			var w  = parseInt(o.root.style.width );
			var h  = parseInt(o.root.style.height);

			if (!o.rMode && o.maxX-o.minX == w && o.maxY-o.minY == h) return false;

			o.onmousemove = null;

			if (o.rMode) {
				o.root.onResizeStart(x, y, w, h, o.rMode, o);

				o.lastMouseX = ex;
				o.lastMouseY = ey;

				o.minMouseX = null;
				o.minMouseY = null;
				o.maxMouseX = null;
				o.maxMouseY = null;

				if (o.rMode & 1) {
					o.maxMouseX = ex + w - o.keepX;
					if (o.minX != null) o.minMouseX = ex - x + o.minX;
				} else if (o.rMode & 2) {
					o.minMouseX = ex - w + o.keepX;
					if (o.maxX != null) o.maxMouseX = ex - x + o.maxX - w;
				}

				if (o.rMode & 4) {
					o.maxMouseY = ey + h - o.keepY;
					if (o.minY != null) o.minMouseY = ey - y + o.minY;
				} else if (o.rMode & 8) {
					o.minMouseY = ey - h + o.keepY;
					if (o.maxX != null) o.maxMouseY = ey - y + o.maxY - h;
				}
			} else {
				o.root.onDragStart(x, y, w, h, 0, o);

				o.lastMouseX = ex;
				o.lastMouseY = ey;

				if (o.minX != null) o.minMouseX = ex - x + o.minX;
				if (o.minY != null) o.minMouseY = ey - y + o.minY;
				if (o.maxX != null) o.maxMouseX = ex - x + o.maxX - w;
				if (o.maxY != null) o.maxMouseY = ey - y + o.maxY - h;
			}
			Resize.obj = o;
			document.onmousemove = (o.rMode) ? Resize.resize : Resize.drag;
			document.onmouseup   = Resize.end;
		}
		return false;
	},

	resize : function(e)
	{
		e = Resize.fixE(e);
		var o = Resize.obj;

		var ex = e.clientX;
		var ey = e.clientY;
		var x  = parseInt(o.root.style.left);
		var y  = parseInt(o.root.style.top);
		var w  = parseInt(o.root.style.width);
		var h  = parseInt(o.root.style.height);
		var nx, ny, nw, nh;

		var mo = o.doMod ? (((o.doAlt   && e.altKey  ) ? 4 : 0) +
		                    ((o.doCtrl  && e.ctrlKey ) ? 2 : 0) +
		                    ((o.doShift && e.shiftKey) ? 1 : 0)) : 0;

		if (o.minMouseX != null) ex = Math.max(ex, o.minMouseX);
		if (o.minMouseY != null) ey = Math.max(ey, o.minMouseY);
		if (o.maxMouseX != null) ex = Math.min(ex, o.maxMouseX);
		if (o.maxMouseY != null) ey = Math.min(ey, o.maxMouseY);

		if (o.rMode & 1) {
			nx = x + ex - o.lastMouseX;
			nw = w + x - nx;
		} else if (o.rMode & 2) {
			nx = x;
			nw = w + ex - o.lastMouseX;
		} else {
			nx = x;
			nw = w;
		}

		if (o.rMode & 4) {
			ny = y + ey - o.lastMouseY;
			nh = h + y - ny;
		} else if (o.rMode & 8) {
			ny = y;
			nh = h + ey - o.lastMouseY;
		} else {
			ny = y;
			nh = h;
		}

		if (o.fMapper){
			var mapped = o.fMapper(x, y, w, h, o.rMode);
			if (mapped[0] != null) x = mapped[0];
			if (mapped[1] != null) y = mapped[1];
			if (mapped[2] != null) w = mapped[2];
			if (mapped[3] != null) h = mapped[3];
		}

		o.root.style.left   = nx + 'px';
		o.root.style.top    = ny + 'px';
		o.root.style.width  = nw + 'px';
		o.root.style.height = nh + 'px';

		o.lastMouseX        = ex;
		o.lastMouseY        = ey;

		o.root.onResize(nx, ny, nw, nh, o.rMode, o);
		return false;
	},

	drag : function(e)
	{
		e = Resize.fixE(e);
		var o = Resize.obj;

		var ex = e.clientX;
		var ey = e.clientY;
		var x  = parseInt(o.root.style.left);
		var y  = parseInt(o.root.style.top);
		var w  = parseInt(o.root.style.width);
		var h  = parseInt(o.root.style.height);
		var nx, ny;

		if (o.minX != null) ex = Math.max(ex, o.minMouseX);
		if (o.minY != null) ey = Math.max(ey, o.minMouseY);
		if (o.maxX != null) ex = Math.min(ex, o.maxMouseX);
		if (o.maxY != null) ey = Math.min(ey, o.maxMouseY);

		nx = x + ex - o.lastMouseX;
		ny = y + ey - o.lastMouseY;

		if (o.fMapper){
			var mapped = o.fMapper(x, y, w, h, 0);
			if (mapped[0] != null) x = mapped[0];
			if (mapped[1] != null) y = mapped[1];
			if (mapped[2] != null) w = mapped[2];
			if (mapped[3] != null) h = mapped[3];
		}

		o.root.style.left = nx + 'px';
		o.root.style.top  = ny + 'px';

		o.lastMouseX      = ex;
		o.lastMouseY      = ey;

		o.root.onDrag(nx, ny, w, h, 0, o);
		return false;
	},

	end : function()
	{
		document.onmousemove = null;
		document.onmouseup   = null;

		var o = Resize.obj;
		((o.rMode) ? o.root.onResizeEnd : o.root.onDragEnd) (
		  parseInt(o.root.style.left),
		  parseInt(o.root.style.top),
		  parseInt(o.root.style.width),
		  parseInt(o.root.style.height),
		  o.rMode,
			o
		);

		o.onmousemove = Resize.update;
		Resize.obj = null;
	},

	disable : function()
	{
		if (Resize.obj != null) Resize.end();
		this.allowResize  = false;
		this.onmousemove  = null;
		this.onmousedown  = null;
		this.style.cursor = this.oCursor;
	},

	enable : function()
	{
		this.onmousemove  = Resize.update;
		this.onmousedown  = Resize.start;
		this.style.cursor = this.oCursor;
		this.allowResize  = true;
	},

	fixE : function(e)
	{
		if (typeof e == 'undefined') e = window.event;
		if (typeof e.layerX == 'undefined') e.layerX = e.offsetX;
		if (typeof e.layerY == 'undefined') e.layerY = e.offsetY;
		return e;
	}

}
