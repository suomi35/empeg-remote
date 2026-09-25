/*
 * empeg weblite by Mark Cushman <mark@cushman.net>
 * modified by Alexander Graf <merlin@ghostwheel.de>
 * (and lots of others not vain enough to put their names here).
 * http://empegbbs.com/ubbthreads.php/ubb/showflat/Number/298495
 */

var here = null; allow_commands = false, allow_files = false;
var VFDcount = 0, pressed = 0, rButton = "", tRepeat = 150, iVFD, tVFD = 250, loadingVFD = 0;
var playlistCache = {}, cachedPlaylists = Array(), maxCache = 20;
var times = Array(65,75,85,100,125,250,500,1000,2000,5000);
var remote = null;
var remdesc = null;
var config = '';

function init(ac, af, type, tagfid, fid, title) {
	allow_commands = ac; allow_files = af;
	// get "refresh"
	var newVFD = getVar('refresh');
	if (newVFD!==null) {
		newVFD = parseInt(newVFD, 10);
		var newTimes = Array();
		for (var i=0, ok=false; i<times.length; i++) {
			if (!ok && newVFD < times[i]) {
				if (newVFD) newTimes[newTimes.length] = newVFD;
				ok = true;
			}
			newTimes[newTimes.length] = times[i];
		}
		if (ok) times = newTimes;
		tVFD = newVFD;
	}
	times[times.length] = 0; // add option for "no refresh"
	// fix images
	fixPNG(document.getElementById('fascia'), 'weblite/fascia.gif');
	fixPNG(document.getElementById('rioimage'), 'weblite/rioremote.gif');
	// init playlist
	here = {
		'type'  : type,
		'tagfid': tagfid,
		'fid'   : fid,
		'title' : '[UP] '+title,
		'length': 0
	};
	document.onkeydown = keydown;
	document.onkeyup = keyup;
	// init sorter
	initSort();
	var sort = getVar('sort');
	if (sort) {
		sortColumns = Array();
		sort = sort.split(',');
		for (var i=0; i<sort.length; i++)
			if (document.getElementById('sort_'+sort[i]))
				sortColumns[sortColumns.length] = sort[i];
		if (sortColumns) {
			sortDirection = getVar('order','desc')=='desc';
			doSort();
		} else {
			sortColumns = null;
		}
	}
	// init parent cache
	if (!parentCache[tagfid]) {
		parentCache[tagfid] = false;
		parentObject = createLine({
			'type'  : 'playlist',
			'tagfid': '101',
			'fid'   : '100',
			'title' : '[UP] All Music',
			'length': 0
		}, true, true);
		var node = sortInsert.parentNode;
		while (node && node.nodeName != 'THEAD') node = node.previousSibling;
		if (node) node.appendChild(parentObject);
	}
	// init fascia selector
	httpGetRequest('weblite/', false, initFasciaSelect);
	// init refresh selector
	initRefreshs();
	// init VFD update
	if (tVFD) startVFD();
	// init remote
	initRemote();
	window.onresize = initRemote;
	toggleRemote(getVar('remote'));
	// init bookmark
	updateBookmark();
}

var hash = null;
function getVar(key, fallback, convert) {
	if (hash===null) hash = window.location.hash.replace(/%23/,'#');
	var res = null;
	var pos = hash.indexOf('#'+key+'=');
	if (pos != -1) {
		pos += key.length+2;
		var end = hash.indexOf('#', pos);
		res = (end != -1)?hash.substr(pos, end-pos):hash.substr(pos);
	}
	if (res !== null && convert !== undefined) res = convert(res);
	if (res === null && fallback !== undefined) res = fallback;
	return res;
}

function initRemote() {
	if (!remote) {
		remote = document.getElementById('rioremote');
		remdesc = document.getElementById('riodesc');
		if (remote) {
			var img = document.getElementById('rioimage');
			remote.style.top = '10px';
			remote.style.left = '590px';
			remote.style.width = img.width+'px';
			remote.style.height = img.height+'px';
			Resize.init(remote);
			remote.minX = 10;
			remote.minY = 10;
			remote.doCur = false;
			remote.style.cursor = 'move';
			if (remdesc.offsetWidth<237) remdesc.style.width = '237px';
		}
	}
	remote.maxX = (window.innerWidth?window.innerWidth:document.body.clientWidth)-20;
	remote.maxY = (window.innerHeight?window.innerHeight:document.body.clientHeight)-40;
}

function nope() {
	this.blur();
	return false;
}

function updateBookmark() {
	var ref = (tVFD!=250)?'#refresh='+(tVFD?tVFD:0):'';
	var fas = document.getElementById('fascia').src.match(/fascia_([^.]+)/);
	fas = fas?'#color='+fas[1]:'';
	var rem = (remote && remote.style.visibility=='visible')?'#remote=1':'';
	var srt = (sortColumns&&sortColumns.length)?('#sort='+sortColumns.join(',')+(sortDirection?'':'#order=asc')):'';
	config = srt+ref+fas+rem;
	document.getElementById('bookmark').href = location.href.split('#').shift()+config;
}

function bookmark() {
	alert('Please right-click or drag this link to your favorites.');
	return nope();
}

function toggleRemote(force) {
	if (remote)
		remote.style.visibility = (force !== undefined)?(force?'visible':'hidden'):((remote.style.visibility=='visible')?'hidden':'visible');
	updateBookmark();
	return nope();
}

function createElement(tag, content, cls, colspan) {
	var res = document.createElement(tag);
	if (cls) res.className = cls;
	if (colspan) res.colSpan = colspan;
	if (typeof content == 'string') {
		res.appendChild(document.createTextNode(content));
	} else if (content) {
		res.appendChild(content);
	}
	return res;
}
function createLink(href, onclick, title, content, direct) {
	var res = createElement('a', content);
	if (href) res.href = href;
	if (onclick) res.onclick = onclick;
	if (title) res.title = title;
	res.direct = direct?true:false;
	return res;
}
function createLine(data, toggle, updir) {
	var line = createElement('TR', null, toggle?'on':'off');
	line.cache = {};
	if (allow_commands) {
		line.appendChild(createElement(
			'TD',
			createLink('?NODATA&SERIAL=%23'+data.fid, doCommand, 'Play', 'play', false),
			'actions'
		));
	}
	switch (data.type) {
		case 'playlist':
			var link = createLink(
				'?FID='+data.tagfid+'&EXT=.xml', loadPlaylist, null, data.title, false
			);
			link.updir = updir?true:false;
			link.id = 'fid_'+data.tagfid;
			line.appendChild(createElement('TD', link, 'playlist', 4));
			line.appendChild(createElement('TD', data.length, 'length'));
			break;
		case 'tune':
			line.appendChild(createElement('TD', data.artist, 'artist'));
			line.appendChild(createElement('TD', data.source, 'source'));
			line.appendChild(createElement('TD', data.title, 'title'));
			line.appendChild(createElement('TD', data.tracknr, 'tracknr'));
			line.appendChild(createElement('TD', data.duration, 'duration'));
			break;
	}
	if (allow_commands) {
		var cell = createElement('TD', null, 'actions');
		cell.appendChild(createLink(
			'?NODATA&SERIAL=%23'+data.fid+'%2B', doCommand, 'Append', 'app', false
		));
		cell.appendChild(createLink(
			'?NODATA&SERIAL=%23'+data.fid+'!', doCommand, 'Insert', 'ins', false
		));
		cell.appendChild(createLink(
			'?NODATA&SERIAL=%23'+data.fid+'-', doCommand, 'Enqueue', 'enq', false
		));
		line.appendChild(cell);
	}
	if (allow_files) {
		var cell = createElement('TD', null, 'actions');
		cell.appendChild(createLink(
			data.title+'.m3u?FID='+data.tagfid+'&EXT=.m3u',
			doCommand, 'Stream', 'strm', true
		));
		if (data.type == 'tune') {
			cell.appendChild(createLink(
				data.artist+' - '+data.title+'.mp3?FID='+data.fid+'&EXT=.mp3',
				doCommand, 'Save File Locally', 'save', true
			));
		}
		line.appendChild(cell);
	}
	return line;
}

function getNode(node, name) {
	var res = node.getElementsByTagName(name);
	return res.length?res[0]:null;
}
function getValue(node, name) {
	var res = node.getElementsByTagName(name);
	return res.length?(res[0].firstChild?res[0].firstChild.data:''):'';
}

var fields = Array(
	'type', 'tagfid', 'fid', 'title', 'length',
	'artist', 'source', 'tracknr', 'duration'
);
var parentCache   = {'101':true}, parentObject = null;
var loadingList   = false;
var playlistTable = null;
var playlistHide  = null;
var playlistAnim  = null;

function hidePlaylist() {
	if (!playlistTable) {
		playlistTable = document.getElementById('playlists');
		playlistHide  = document.getElementById('hide');
		playlistAnim  = document.getElementById('anim');
	}
	if (playlistHide) {
		playlistHide.style.top     = playlistTable.offsetTop+'px';
		playlistHide.style.left    = playlistTable.offsetLeft+'px';
		playlistHide.style.width   = Math.max(32,playlistTable.offsetWidth)+'px';
		playlistHide.style.height  = Math.max(32,playlistTable.offsetHeight)+'px';
		playlistAnim.style.marginTop = Math.max(0,playlistTable.offsetHeight/2-16)+'px';
		moveAnim(true);
		playlistHide.style.display = 'block';
	}
}
function moveAnim(init) {
	var tt = playlistTable.offsetTop;
	var tb = tt + playlistTable.offsetHeight;
	var wt = window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop;
	var wb = wt + (window.innerHeight||document.body.clientHeight);
	if (init===true){
		if ((tt<wt) || (tb>wb)) {
			window.onscroll = moveAnim;
			playlistAnim.style.marginTop = Math.min(tb-tt-32,Math.max(0,parseInt(wt-tt+(wb-wt)/2-16)))+'px';
		}
	} else {
		playlistAnim.style.marginTop = Math.min(tb-tt-32,Math.max(0,parseInt(wt-tt+(wb-wt)/2-16)))+'px';
	}
}

function showPlaylist() {
	if (playlistHide) {
		window.onscroll = null;
		playlistHide.style.display = 'none';
	}
}

function loadPlaylist(obj, nosave) {
	if (loadingList) return false;

	loadingList = true;

	var fid = null;
	var updir = false;
	if (typeof obj == 'string' || typeof obj == 'number') {
		fid = obj;
	} else if (obj === undefined) {
		fid = window.event.srcElement.id.split('_')[1];
		updir = window.event.srcElement.updir;
	} else if (!obj.id) {
		fid = obj.target.id.split('_')[1];
		updir = obj.target.updir;
	} else {
		fid = obj.id.split('_')[1];
		updir = obj.updir;
	}
	if (fid) {
		if (!nosave && here && !updir && !parentCache[fid]) parentCache[fid] = here;
		if (playlistCache[fid]) {
			usePlaylist(fid);
		} else {
			hidePlaylist();
			httpGetRequest('?FID='+fid+'&EXT=.xml', true, usePlaylist);
		}
	} else {
		loadingList = false;
	}
	return false;
}
function usePlaylist(xml) {

	if (typeof xml == 'string') {
		var playlist = playlistCache[xml];
	} else {
		try {
			var playlist = getNode(xml, 'playlist');
			var tagfid = playlist.getAttribute('tagfid');
			playlistCache[tagfid] = playlist;
		} catch(e) {
			showPlaylist();
			alert('Invalid playlist!');
			loadingList = false;
			return false;
		}
	}

	var cleaned = Array();
	for (var c=0; c<cachedPlaylists.length; c++)
		if (cachedPlaylists[c] != tagfid)
			cleaned[cleaned.length] = cachedPlaylists[c];
	cleaned[cleaned.length] = tagfid;
	if (cleaned.length > maxCache)
		cleaned.shift();
	cachedPlaylists = cleaned;

	document.getElementById('title').firstChild.data = playlist.getAttribute('title');
	here = {
		'type'  : playlist.getAttribute('type'),
		'tagfid': playlist.getAttribute('tagfid'),
		'fid'   : playlist.getAttribute('fid'),
		'title' : '[UP] '+playlist.getAttribute('title'),
		'length': 0
	};

	if (parentObject) parentObject.parentNode.removeChild(parentObject);

	var parent_data = parentCache[here['tagfid']];
	if (parent_data===false) {
		parentObject = createLine({
			'type'  : 'playlist',
			'tagfid': '101',
			'fid'   : '100',
			'title' : '[UP] All Music',
			'length': 0
		}, true, true);
	} else if (parent_data && parent_data!==true) {
		parentObject = createLine(parent_data, true, true);
	} else {
		parentObject = null;
	}
	if (parentObject!==null) {
		var node = sortInsert.parentNode;
		while (node && node.nodeName != 'THEAD') node = node.previousSibling;
		if (node) node.appendChild(parentObject);
	}

	sortRows = Array();

	var items  = getNode(playlist, 'items');
	var item   = items.firstChild;
	var node   = sortInsert.parentNode.firstChild, next = null;
	var line   = null;
	var data   = null;
	var toggle = false;

	while (item) {
		if (item.nodeName == 'item') {
			data = {};
			for (var i=0; i<fields.length; i++) data[fields[i]] = getValue(item, fields[i]);
			line = createLine(data, toggle);
			sortRows[sortRows.length] = line;
			if (node == sortInsert) {
				node.parentNode.insertBefore(line, node);
			} else {
				next = node.nextSibling;
				node.parentNode.replaceChild(line, node);
				node = next;
			}
			toggle = !toggle;
		}
		item = item.nextSibling;
	}
	while (node != sortInsert) {
		next = node.nextSibling;
		node.parentNode.removeChild(node);
		node = next;
	}
	sortColumns = Array();
	sortDirection = null;
	showSort();
	showPlaylist();
	loadingList = false;
}

function httpGetRequest(url, xml, callback) {
	if (window.XMLHttpRequest)
		var xmlhttp = new XMLHttpRequest();
	else if (window.ActiveXObject)
		var xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	xmlhttp.open("GET", url, true);
	xmlhttp.onreadystatechange = function() {
		if (xmlhttp.readyState != 4) return;
		if (callback != null) callback(xml ? xmlhttp.responseXML : xmlhttp.responseText);
	};
	xmlhttp.send(null);
}

function doCommand(obj) {
	var href = null, direct = false;
	if (obj === undefined) {
		href = window.event.srcElement.href;
		direct = window.event.srcElement.direct;
	} else if (!obj.href) {
		href = obj.target.href;
		direct = obj.target.direct;
	} else {
		href = obj.href;
		direct = obj.direct;
	}
	if (href) {
		if (direct) {
			window.location.href = href;
		} else {
			httpGetRequest(href, false, null);
		}
	}
	return false;
}

var png4msie = false;
var gif4msie = false;
try {
	var png4msie = navigator.platform == "Win32" && navigator.appName == "Microsoft Internet Explorer";
	if (png4msie) {
		gif4msie = true;
		png4msie = false;
		var match = navigator.appVersion.match(/MSIE (\d+\.\d+)/, '');
		if (match) {
			var version = parseInt(match[1]);
			png4msie = (version >= 5.5) && (version < 7);
			gif4msie = version < 7;
		}
	}
} catch(e) {}

function fixPNG(myImage, fallback) {
	myImage.onload = null;
	myImage.png4msie = false;
	if (png4msie) {
		try {
			if (document.body.filters) {
				myImage.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+myImage.src+"',sizingMethod='scale')";
				myImage.src = 'weblite/empty.gif';
				myImage.png4msie = true;
			} else {
				myImage.src = fallback;
			}
		} catch (e) {
			myImage.src = fallback;
		}
	} else if (gif4msie) {
		myImage.src = fallback;
	}
}

function ignoreClick() {
	return false;
}

function pressButton(button) {
	if (!pressed) {
		pressed = 1;
		httpGetRequest("?NODATA&BUTTONRAW=" + button, false, null);
	}
	return false;
}

function releaseButton(button, cleardesc) {
	if (pressed) {
		pressed = 0;
		httpGetRequest("?NODATA&BUTTONRAW=" + button + ".R", false, null);
	}
	stopRepeat();
	if (cleardesc) describeButton('');
	return false;
}

function describeButton(text) {
	if (remdesc) {
		if (text) {
			remdesc.innerHTML = '<b>'+text.replace(/\.\./g,'</b><br/>').replace(/\n/g,'<br/><b>').replace(/: /g,':</b> ');
			remdesc.style.visibility = 'visible';
			remote.style.cursor = 'pointer';
		} else {
			remdesc.style.visibility = 'hidden';
			remote.style.cursor = 'move';
		}
	}
}

function repeatButton() {
	if (rButton) {
		pressed = 0;
		pressButton(rButton);
		setTimeout('repeatButton()', tRepeat);
	}
}

function startRepeat(button, interval) {
	if (!rButton) {
		rButton = button;
		if (interval) tRepeat = interval;
		repeatButton();
	}
	return false;
}

function stopRepeat() {
	if (rButton) {
		rButton = "";
		pressed = 0;
	}
	return false;
}

function doneVFD() {
	loadingVFD = 0;
}

function startVFD(tm) {
	if (iVFD !== null) clearInterval(iVFD);
	if (tm !== undefined) tVFD = tm;
	loadingVFD = 0;
	if (tVFD) {
		iVFD = setInterval("refreshVFD()",tVFD);
	} else {
		iVFD = null;
	}
}

function stopVFD() {
	return startVFD(0);
}

function initRefreshs() {
	var refreshs = document.getElementById('refreshs');
	var button = document.getElementById('refreshbutton');
	if (refreshs && button && document.createElement) {
		for (var c=0,s=0; c<times.length; c++) {
			if (times[c] == tVFD) s=c;
			var opt = document.createElement('OPTION');
			opt.value = times[c];
			var txt = times[c]?(times[c]/1000+' s'):'no refresh';
			opt.appendChild(document.createTextNode(txt));
			refreshs.appendChild(opt);
		}
		refreshs.selectedIndex = s;
		refreshs.size = times.length;
		button.slaveElement = refreshs;
		button.onmouseover = overButton;
		button.onmouseout  = outButton;
		refreshs.style.left = (button.offsetLeft+17)+'px';
		refreshs.slaveElement = refreshs;
		refreshs.onmouseover = overButton;
		refreshs.onmouseout  = outButton;
	}
}
function selectRefresh(select) {
	startVFD(parseInt(select.options[select.selectedIndex].value));
	updateBookmark();
	select.blur();
}

function refreshVFD() {
	if (loadingVFD <= 0) {
		loadingVFD = 1000/tVFD;
		document.images.vfd.src = '/proc/empeg_screen.png?IGNORE=' + Date() + VFDcount++;
		document.images.vfd.onload  = doneVFD;
		document.images.vfd.onabort = doneVFD;
		document.images.vfd.onerror = stopVFD;
	} else {
		--loadingVFD;
	}
}

function keydown(e) {
	var key = (e===undefined)?window.event.keyCode:e.which;
	switch (key) {
		case 33: // Page Up
			return startRepeat('KnobLeft', 125);
		case 34: // Page Down
			return startRepeat('KnobRight', 125);
		case 12: // 5
		case 35: // Pos 1
		case 36: // End
			return pressButton('Knob');
		case 37: // Left
			return pressButton('Left');
		case 38: // Up
			return pressButton('Top');
		case 39: // Right
			return pressButton('Right');
		case 40: // Down
			return pressButton('Bottom');
	}
	return true;
}
function keyup(e) {
	var key = (e===undefined)?window.event.keyCode:e.which;
	switch (key) {
		case 33:
			return stopRepeat();
		case 34:
			return stopRepeat();
		case 12:
		case 35:
		case 36:
			return releaseButton('Knob');
		case 37:
			return releaseButton('Left');
		case 38:
			return releaseButton('Top');
		case 39:
			return releaseButton('Right');
		case 40:
			return releaseButton('Bottom');
	}
	return true;
}

function defSort(a, b) {
	return a<b?-1:a>b;
}
function parseNum(v) {
	if (v.indexOf('/') != -1) { // tracknumber with tracktotal
		v = v.split('/');
		return parseInt(v[0],10)*1000000 + parseInt(v[1],10);
	} else if (v.indexOf(':') != -1) { // time
		v = v.split(':');
		return parseInt(v[0],10)*60 + parseInt(v[1],10);
	} else { // playlist-length or tracknumber without tracktotal
		return parseInt(v,10)-1000000;
	}
}
function numSort(a, b) {
	a = parseNum(a);
	b = parseNum(b);
	return a<b?-1:a>b;
}

var sortRows = null, sortInsert = null, sortColumns = null, sortDirection = null;
var sortFunctions = {
	'artist'	: defSort,
	'playlist'	: defSort,
	'source'	: defSort,
	'title'		: defSort,
	'tracknr'	: numSort,
	'duration'	: numSort,
	'length'	: numSort
}

function getText(obj) {
	var res = '';
	for (var i=0; i<obj.childNodes.length; i++) {
		var child = obj.childNodes[i];
		if (child.nodeType == 3) {
			res += child.nodeValue;
		} else if (child.nodeType == 1) {
			res += ' '+getText(child);
		}
	}
	return res.replace(/(^ +| +$)/,'').replace(/ +/,' ');
}
function getColumn(row, names) {
	var res = row.cache[names[0]];
	if (!res) {
		var first = true;
		for (var c=0; c<row.cells.length; c++) {
			if (first && sortFunctions[row.cells[c].className]) {
				first = false;
				res = Array(-1000000, row.cells[c].className, getText(row.cells[c]));
			}
			for (var n=0; n<names.length; n++)
				if (row.cells[c].className == names[n]) {
					res = Array(-n, names[n], getText(row.cells[c]));
					break;
				}
		}
		row.cache[names[0]] = res;
	}
	return res;
}

function rowSort(ra, rb) {
	var ca = getColumn(ra, sortColumns);
	var cb = getColumn(rb, sortColumns);
	if (ca[1] == cb[1]) {
		return sortFunctions[ca[1]](ca[2], cb[2]);
	} else {
		return defSort(ca[0], cb[0]);
	}
}

function initSort() {
	sortRows = Array();
	var table = document.getElementById('playlists');
	for (var r=0; r<table.rows.length; r++) {
		if (table.rows[r].parentNode.nodeName == 'THEAD') continue;
		var line = table.rows[r].className;
		if (line=='on' || line == 'off') {
			table.rows[r].cache = {};
			sortRows[sortRows.length] = table.rows[r];
		} else {
			sortInsert = table.rows[r];
			break;
		}
	}
}

function doSort(/*, column, column, ... */) {
	if (!arguments.length) {
		if (!sortColumns) return;
		var columns = sortColumns;
		sortRows.sort(rowSort);
		if (!sortDirection) sortRows.reverse();
	} else {
		var columns = Array();
		for (var i=0; i<arguments.length; i++) columns[i]=arguments[i];
		if (sortColumns && columns[0] == sortColumns[0]) {
			sortDirection = !sortDirection;
			sortRows.reverse();
		} else {
			sortColumns = columns;
			sortDirection = true;
			sortRows.sort(rowSort);
		}
	}
	for (var r=0; r<sortRows.length; r++) {
		sortInsert.parentNode.insertBefore(sortRows[r], sortInsert);
		sortRows[r].className = r%2?'on':'off';
	}
	showSort();
}

function showSort() {
	var obj = document.getElementById('sorts').firstChild;
	for (var node=obj.parentNode.firstChild; node; node=node.nextSibling)
		if (node.innerHTML)
			node.innerHTML = node.innerHTML.replace(/ [^ ]+$/,'');
	if (sortColumns && sortColumns.length) {
		var obj = document.getElementById('sort_'+sortColumns[0]);
		obj.innerHTML += sortDirection?' &dArr;':' &uArr;';
	}
	updateBookmark();
}

function initFasciaSelect(html) {
	var fascias = document.getElementById('fascias');
	var button = document.getElementById('colorbutton');
	if (fascias && button && document.createElement) {
		var found = Array();
		while (true) {
			var match = html.match(/[\/"](fascia(_[^"]+)?\.(png|gif))"/);
			if (!match) break;
			found[found.length] = Array(match[3]=='png'?0:1, match[1]);
			html = html.substr(match.index + match[0].length);
		}
		found.sort();
		var current = document.getElementById('fascia').src;
		var selected = getVar('color');
		if (selected) selected = 'fascia_'+selected+'.'+current.split('.').pop();
		var other = null;
		for (var i=0,s=0; i<found.length; i++) {
			if (found[i][1] == selected) other=i;
			if (current.indexOf('/'+found[i][1])!=-1) s=i;
			fascias.appendChild(createFascia(found[i][1]));
		}
		if (other !== null) {
			fascias.selectedIndex = other;
			selectFascia(fascias);
		} else {
			fascias.selectedIndex = s;
		}
		fascias.size = found.length;
		button.slaveElement = fascias;
		button.onmouseover = overButton;
		button.onmouseout  = outButton;
		fascias.style.left = (button.offsetLeft+17)+'px';
		fascias.slaveElement = fascias;
		fascias.onmouseover = overButton;
		fascias.onmouseout  = outButton;
	}
}
function createFascia(image, desc) {
	if (!desc) {
		var match = image.match(/^fascia_?([^"]*)\.(png|gif)$/);
		if (match) {
			if (match[1]) {
				desc = match[1].split('_');
				for (var i=0; i<desc.length; i++)
					desc[i] = desc[i].substr(0,1).toUpperCase() + desc[i].substr(1).toLowerCase();
			} else {
				desc = Array('Default');
			}
			if (match[2] != 'png') desc[desc.length] = '['+match[2].toUpperCase()+']';
			desc = desc.join(' ');
		} else {
			desc = image;
		}
	}
	var opt = document.createElement('OPTION');
	opt.value = 'weblite/'+image;
	opt.appendChild(document.createTextNode(desc));
	return opt;
}

function selectFascia(select) {
	var myImage = document.getElementById('fascia');
	var newSource = select.options[select.selectedIndex].value;
	if (myImage.png4msie) {
		if (newSource.indexOf('.png')==-1) {
			myImage.style.filter = null;
			myImage.src = newSource;
		} else {
			myImage.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+newSource+"',sizingMethod='scale')";
			myImage.src = 'weblite/empty.gif';
		}
	} else {
		myImage.src = newSource;
	}
	updateBookmark();
	select.blur();
}

var buttonTimer = null;
var buttonElement = null;
function overButton() {
	clearButton();
	buttonElement = this.slaveElement;
	buttonElement.style.visibility = 'visible';
}
function outButton() {
	buttonTimer = window.setTimeout('clearButton()',250);
}

function clearButton() {
	if (buttonTimer) {
		window.clearTimeout(buttonTimer);
		buttonTimer = null;
	}
	if (buttonElement) {
		buttonElement.style.visibility = 'hidden';
		buttonElement = null;
	}
}

