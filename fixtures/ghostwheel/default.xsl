<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:variable name="allow_commands" select="//playlist/@allow_commands"/>
<xsl:variable name="allow_files" select="//playlist/@allow_files"/>
<xsl:template match="playlist">
<html>
<head>
	<title>empeg web lite - <xsl:value-of select="@title"/></title>
	<script type="text/javascript" language="javascript" src="weblite/weblite.js"></script>
	<script type="text/javascript" language="javascript" src="weblite/domresize.js"></script>
	<link rel="stylesheet" type="text/css" href="weblite/weblite.css"/>
</head>
<body>
<xsl:attribute name="onload">init(<xsl:value-of select="$allow_commands"/>,<xsl:value-of select="$allow_files"/>,'<xsl:value-of select="@type"/>','<xsl:value-of select="@tagfid"/>','<xsl:value-of select="@fid"/>','<xsl:value-of select="@title"/>')</xsl:attribute>
	<div class="screen">
		<img id="screen" src="/proc/empeg_screen.gif" name="vfd" width="256" height="64" />
	</div>
	<div class="fascia">
		<xsl:choose>
			<xsl:when test="$allow_commands = 1">
				<map id="buttons" name="buttons">
					<area shape="circle" coords="80,53 10"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return pressButton('Top')"
						  onmouseup="return releaseButton('Top')"
						 onmouseout="return releaseButton('Top')" />
					<area shape="polygon" coords="39,89 48,77 77,72 77,97 70,101 51,98"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return pressButton('Left')"
						  onmouseup="return releaseButton('Left')"
						 onmouseout="return releaseButton('Left')" />
					<area shape="polygon" coords="84,72 111,76 123,87 112,96 91,101 84,96"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return pressButton('Right')"
						  onmouseup="return releaseButton('Right')"
						 onmouseout="return releaseButton('Right')" />
					<area shape="polygon" coords="81,103 89,108 93,107 90,126 81,144 71,128 69,107 72,107"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return pressButton('Bottom')"
						  onmouseup="return releaseButton('Bottom')"
						 onmouseout="return releaseButton('Bottom')" />
					<area shape="circle"  coords="476,104 14"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return pressButton('Knob')"
						  onmouseup="return releaseButton('Knob')"
						 onmouseout="return releaseButton('Knob')" />
					<area shape="polygon" coords="467,83 461,88 456,94 454,102 456,111 460,117 466,122 463,129 454,124 447,115 444,103 447,88 454,80 463,75"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return startRepeat('KnobLeft', 300)"
						  onmouseup="return stopRepeat()"
						 onmouseout="return stopRepeat()" />
					<area shape="polygon" coords="483,84 490,88 494,94 496,102 495,111 491,117 485,121 488,128 496,124 502,114 504,102 502,90 496,82 487,76"
						       href="?NODATA"
						    onclick="return ignoreClick()"
						onmousedown="return startRepeat('KnobRight', 300)"
						  onmouseup="return stopRepeat()"
						 onmouseout="return stopRepeat()" />
				</map>
				<img id="fascia" src="weblite/fascia.png" width="572" height="172" usemap="#buttons" alt=""/>
				<div class="control">
					<a href="." id="bookmark" onclick="return bookmark()">bookmark view</a>
					<a href="." onclick="return nope()" id="refreshbutton">toggle refresh</a>
					<a href="." onclick="return nope()" id="colorbutton">change color</a>
					<a href="." onclick="return toggleRemote()">show/hide remote</a>
				</div>
			</xsl:when>
			<xsl:when test="$allow_commands = 0">
				<img id="fascia" src="weblite/fascia.png" width="572" height="172" alt="0"/>
				<div class="control">
					<a href="." id="bookmark" onclick="return bookmark()">bookmark view</a>
					<a href="." onclick="return nope()" id="refreshbutton">toggle refresh</a>
					<a href="." onclick="return nope()" id="colorbutton">change color</a>
				</div>
			</xsl:when>
		</xsl:choose>
	</div>
	<xsl:choose>
		<xsl:when test="$allow_commands = 1">
			<map id="riobuttons" name="riobuttons">
				<area shape="circle" coords="37,55 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('One')"
					 onmouseout="return releaseButton('One',true)"
					onmouseover="return describeButton('Rio 1 Button..Selects time format.\nIn menu: Selects 1st item.\nText entry: Space.\nTuner: Preset 1.')"
					onmousedown="return pressButton('One')" />
				<area shape="circle" coords="91,55 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Two')"
					 onmouseout="return releaseButton('Two',true)"
					onmouseover="return describeButton('Rio 2 Button..Tweak order by Artist.\nHold: Hate Artist.\nIn menu: Selects 2nd item.\nPlaylists: Selects ABC.\nText entry: ABC.\nTuner: Preset 2.')"
					onmousedown="return pressButton('Two')" />
				<area shape="circle" coords="145,55 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Three')"
					 onmouseout="return releaseButton('Three',true)"
					onmouseover="return describeButton('Rio 3 Button..Tweak order by Album.\nHold: Hate Album.\nIn menu: Selects 3rd item.\nPlaylists: Selects DEF.\nText entry: DEF.\nTuner: Preset 3.')"
					onmousedown="return pressButton('Three')" />
				<area shape="circle" coords="198,55 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Source')"
					 onmouseout="return releaseButton('Source',true)"
					onmouseover="return describeButton('Rio Source/Power Button..Selects Player/Aux.\nHold: Power Off.')"
					onmousedown="return pressButton('Source')" />
				<area shape="circle" coords="37,109 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Four')"
					 onmouseout="return releaseButton('Four',true)"
					onmouseover="return describeButton('Rio 4 Button..Mark Track.\nIn menu: Selects 4th item.\nPlaylists: Selects GHI.\nText entry: GHI.\nTuner: Preset 4.')"
					onmousedown="return pressButton('Four')" />
				<area shape="circle" coords="91,109 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Five')"
					 onmouseout="return releaseButton('Five',true)"
					onmouseover="return describeButton('Rio 5 Button..Tweak order by Genre.\nHold: Hate Genre.\nIn menu: Selects 5th item.\nPlaylists: Selects JKL.\nText entry: JKL.\nTuner: Preset 5.')"
					onmousedown="return pressButton('Five')" />
				<area shape="circle" coords="145,109 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Six')"
					 onmouseout="return releaseButton('Six',true)"
					onmouseover="return describeButton('Rio 6 Button..Tweak order by Year.\nIn menu: Selects 6th item.\nPlaylists: Selects MNO.\nText entry: MNO.\nTuner: Preset 6.')"
					onmousedown="return pressButton('Six')" />
				<area shape="circle" coords="199,109 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Tuner')"
					 onmouseout="return releaseButton('Tuner',true)"
					onmouseover="return describeButton('Rio Tuner/Bank Button..Selects Tuner.\nHold: Selects preset bank.\nTuner: AM/FM.')"
					onmousedown="return pressButton('Tuner')" />
				<area shape="circle" coords="37,162 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Seven')"
					 onmouseout="return releaseButton('Seven',true)"
					onmouseover="return describeButton('Rio 7 Button..Select repeat mode.\nIn menu: Selects 7th item.\nPlaylists: Selects PRS.\nText entry: PRS.\nTuner: Preset 7.')"
					onmousedown="return pressButton('Seven')" />
				<area shape="circle" coords="91,162 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Eight')"
					 onmouseout="return releaseButton('Eight',true)"
					onmouseover="return describeButton('Rio 8 Button..Swap next.\nIn menu: Selects 8th item.\nPlaylists: Selects TUV.\nText entry: TUV.\nTuner: Preset 8.')"
					onmousedown="return pressButton('Eight')" />
				<area shape="circle" coords="145,162 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Nine')"
					 onmouseout="return releaseButton('Nine',true)"
					onmouseover="return describeButton('Rio 9 Button..Reserved for future expansion.\nIn menu: Selects 9th item.\nPlaylists: Selects WXY.\nText entry: WXY.\nTuner: Preset 9.')"
					onmousedown="return pressButton('Nine')" />
				<area shape="circle" coords="199,162 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('SelectMode')"
					 onmouseout="return releaseButton('SelectMode',true)"
					onmouseover="return describeButton('Rio Select Mode Button..Re-seeds the current visual.\nIn search: Selects Replace, Append, or Insert mode for searching.')"
					onmousedown="return pressButton('SelectMode')" />
				<area shape="circle" coords="37,217 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Cancel')"
					 onmouseout="return releaseButton('Cancel',true)"
					onmouseover="return describeButton('Rio Cancel Button..Mark Track.\nIn menu: Cancel.\nIn search: Backspace.')"
					onmousedown="return pressButton('Cancel')" />
				<area shape="circle" coords="91,216 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Zero')"
					 onmouseout="return releaseButton('Zero',true)"
					onmouseover="return describeButton('Rio 0 Button..Shuffle.\nIn menu: Selects 10th item.\nPlaylists: Selects QZ.\nText entry: QZ.\nTuner: Preset 0.')"
					onmousedown="return pressButton('Zero')" />
				<area shape="circle" coords="145,217 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Search')"
					 onmouseout="return releaseButton('Search',true)"
					onmouseover="return describeButton('Rio Search Button..PIN/Search (press multiple).\nTuner: Type in a frequency.\nHold: Search by Title.')"
					onmousedown="return pressButton('Search')" />
				<area shape="circle" coords="199,217 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Sound')"
					 onmouseout="return releaseButton('Sound',true)"
					onmouseover="return describeButton('Rio Sound/Equalizer Button..Opens the Sound menu.\nHold: Opens the equalizer.')"
					onmousedown="return pressButton('Sound')" />
				<area shape="circle" coords="37,271 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('PrevTrack')"
					 onmouseout="return releaseButton('PrevTrack',true)"
					onmouseover="return describeButton('Rio |&lt;&lt; Button..Previous track.\nHold: Rewind.\nIn menu: Move left/up.\nText entry: Backspace.\nTuner: Decrement frequency.\nTuner hold: Scan frequency.')"
					onmousedown="return pressButton('PrevTrack')" />
				<area shape="circle" coords="91,271 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('NextTrack')"
					 onmouseout="return releaseButton('NextTrack',true)"
					onmouseover="return describeButton('Rio &gt;&gt;| Button..Next track.\nHold: Fast Forward.\nIn menu: Move right/down.\nTuner: Increment frequency.\nTuner hold: Scan frequency.')"
					onmousedown="return pressButton('NextTrack')" />
				<area shape="circle" coords="145,271 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Menu')"
					 onmouseout="return releaseButton('Menu',true)"
					onmouseover="return describeButton('Rio Menu/OK Button..Opens the main menu.\nIn most screens: OK/enter.\nIn EQ editor: With a band selected, selects Q and bandwidth parameters.\nHold on Playlists menu: Jump down to last-selected playlist.\nHold on a song or a playlist: Insert or Append that item.')"
					onmousedown="return pressButton('Menu')" />
				<area shape="circle" coords="199,271 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('VolUp')"
					 onmouseout="return releaseButton('VolUp',true)"
					onmouseover="return describeButton('Rio Vol. Up Button..Increase volume.\nIn some screens: Value +.')"
					onmousedown="return pressButton('VolUp')" />
				<area shape="circle" coords="37,325 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Info')"
					 onmouseout="return releaseButton('Info',true)"
					onmouseover="return describeButton('Rio Info Button..Select Info screen mode.\nHold: Track details.')"
					onmousedown="return pressButton('Info')" />
				<area shape="circle" coords="91,325 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Visual')"
					 onmouseout="return releaseButton('Visual',true)"
					onmouseover="return describeButton('Rio Visual Button..Change current visual.\nHold: Toggle between favorite visuals mode and all visuals mode (requires config.ini edit).')"
					onmousedown="return pressButton('Visual')" />
				<area shape="circle" coords="145,325 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('Play')"
					 onmouseout="return releaseButton('Play',true)"
					onmouseover="return describeButton('Rio Play/Pause Button..Play/Pause.\nHold: Hush/unhush.')"
					onmousedown="return pressButton('Play')" />
				<area shape="circle" coords="199,325 20"
								 href="?NODATA"
							onclick="return ignoreClick()"
						onmouseup="return releaseButton('VolDown')"
					 onmouseout="return releaseButton('VolDown',true)"
					onmouseover="return describeButton('Rio Vol. Down Button..Decrease volume.\nIn some screens: Value -.')"
					onmousedown="return pressButton('VolDown')" />
			</map>
			<div id="rioremote">
				<img id="rioimage" src="weblite/rioremote.png" width="237" height="385" usemap="#riobuttons" alt=""/>
				<div id="riodesc"/>
			</div>
		</xsl:when>
		<xsl:when test="$allow_commands = 0">
			<div id="rioremote">
				<img id="rioimage" src="weblite/rioremote.png" width="237" height="385" alt=""/>
				<div id="riodesc"/>
			</div>
		</xsl:when>
	</xsl:choose>
	<select id="fascias" class="fascia" size="200" onchange="selectFascia(this)"></select>
	<select id="refreshs" class="refresh" size="200" onchange="selectRefresh(this)"></select>
	<div id="hide"><img src="weblite/loader.gif" id="anim" alt=""/></div>
	<table id="playlists">
		<thead>
			<tr>
				<th id="title" class="title">
					<xsl:attribute name="colspan"><xsl:value-of select="5 + ($allow_commands * 2) + $allow_files"/></xsl:attribute>
					<xsl:value-of select="@title"/>
				</th>
			</tr>
			<tr id="sorts">
				<xsl:choose><xsl:when test="$allow_commands = 1">
					<th>Sort:</th>
				</xsl:when></xsl:choose>
				<th class="action" id="sort_artist" title="Sort by Artist" onclick="doSort('artist','playlist')">Artist</th>
				<th class="action" id="sort_source" title="Sort by Source" onclick="doSort('source')">Source</th>
				<th class="action" id="sort_title" title="Sort by Title" onclick="doSort('title')">Title</th>
				<th class="action" id="sort_tracknr" title="Sort by Track" onclick="doSort('tracknr')">Track</th>
				<th class="action" id="sort_duration" title="Sort by Length" onclick="doSort('duration','length')">Length</th>
				<xsl:choose><xsl:when test="($allow_commands = 1) or ($allow_files = 1)">
					<th><xsl:attribute name="colspan"><xsl:value-of select="$allow_files + $allow_commands"/></xsl:attribute>Actions</th>
				</xsl:when></xsl:choose>
			</tr>
		</thead>
		<tbody>
			<xsl:apply-templates select="items"/>
			<tr><td class="footer">
					<xsl:attribute name="colspan"><xsl:value-of select="5 + ($allow_commands * 2) + $allow_files"/></xsl:attribute>
					<span id="footerlinks"><a href="?FID=101&amp;EXT=.xml" id="root_101" onclick="return loadPlaylist(this, true)">root playlist</a></span>
					<span id="tagline"><a href="http://empegbbs.com/ubbthreads.php/ubb/showflat/Number/298495">empeg web lite 0.95</a></span>
			</td></tr>
		</tbody>
	</table>
</body>
</html>
</xsl:template>
<xsl:template match="items">
	<xsl:for-each select="item">
	<tr>
		<xsl:attribute name="class">
			<xsl:choose>
				<xsl:when test="(position() mod 2 = 0)">on</xsl:when>
				<xsl:when test="(position() mod 2 = 1)">off</xsl:when>
			</xsl:choose>
		</xsl:attribute>
		<xsl:choose>
			<xsl:when test="$allow_commands = 1">
				<td class="actions">
					<a><xsl:attribute name="href">?NODATA&amp;SERIAL=%23<xsl:value-of select="fid"/></xsl:attribute>
					<xsl:attribute name="onclick">return doCommand(this)</xsl:attribute>
					<xsl:attribute name="title">Play</xsl:attribute>play</a>
				</td>
			</xsl:when>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="type = 'playlist'">
				<td class="playlist" colspan="4">
					<a><xsl:attribute name="href">?FID=<xsl:value-of select="tagfid"/>&amp;EXT=.xml</xsl:attribute>
					<xsl:attribute name="onclick">return loadPlaylist(this)</xsl:attribute>
					<xsl:attribute name="id">fid_<xsl:value-of select="tagfid"/></xsl:attribute>
					<xsl:value-of select="title"/></a></td>
				<td class="length"><xsl:value-of select="length"/></td>
			</xsl:when>
			<xsl:when test="type = 'tune'">
				<td class="artist"><xsl:value-of select="artist"/></td>
				<td class="source"><xsl:value-of select="source"/></td>
				<td class="title"><xsl:value-of select="title"/></td>
				<td class="tracknr"><xsl:value-of select="tracknr"/></td>
				<td class="duration"><xsl:value-of select="duration"/></td>
			</xsl:when>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="$allow_commands = 1">
				<td class="actions">
					<a><xsl:attribute name="href">?NODATA&amp;SERIAL=%23<xsl:value-of select="fid"/>%2B</xsl:attribute>
					<xsl:attribute name="onclick">return doCommand(this)</xsl:attribute>
					<xsl:attribute name="title">Append</xsl:attribute>app</a>
					<a><xsl:attribute name="href">?NODATA&amp;SERIAL=%23<xsl:value-of select="fid"/>!</xsl:attribute>
					<xsl:attribute name="onclick">return doCommand(this)</xsl:attribute>
					<xsl:attribute name="title">Insert</xsl:attribute>ins</a>
					<a><xsl:attribute name="href">?NODATA&amp;SERIAL=%23<xsl:value-of select="fid"/>-</xsl:attribute>
					<xsl:attribute name="onclick">return doCommand(this)</xsl:attribute>
					<xsl:attribute name="title">Enqueue</xsl:attribute>enq</a>
				</td>
			</xsl:when>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="$allow_files = 1">
				<td class="actions">
					<a><xsl:attribute name="href">
						<xsl:value-of select="title"/>.m3u?FID=
						<xsl:value-of select="tagfid"/>&amp;EXT=.m3u
					</xsl:attribute>
					<xsl:attribute name="title">Stream</xsl:attribute>strm</a>
					<xsl:choose>
						<xsl:when test="(type = 'tune')">
							<a><xsl:attribute name="href">
								<xsl:value-of select="artist"/>%20-%20
								<xsl:value-of select="title"/>.mp3?FID=
								<xsl:value-of select="fid"/>&amp;EXT=.mp3
							</xsl:attribute>
							<xsl:attribute name="title">Save File Locally</xsl:attribute>save</a>
						</xsl:when>
					</xsl:choose>
				</td>
			</xsl:when>
		</xsl:choose>
	</tr>
	</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
