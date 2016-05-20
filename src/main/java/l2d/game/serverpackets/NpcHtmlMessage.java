package l2d.game.serverpackets;

import java.util.ArrayList;

import l2d.ext.scripts.Scripts;
import l2d.ext.scripts.Scripts.ScriptClassAndMethod;
import l2d.game.model.L2Player;
import l2d.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.EpicRespawnTimesHolder;
import l2d.util.Files;
import l2d.util.Strings;

/**
 * the HTML parser in the client knowns these standard and non-standard tags and attributes
 * VOLUMN
 * UNKNOWN
 * UL
 * U
 * TT
 * TR
 * TITLE
 * TEXTCODE
 * TEXTAREA
 * TD
 * TABLE
 * SUP
 * SUB
 * STRIKE
 * SPIN
 * SELECT
 * RIGHT
 * PRE
 * P
 * OPTION
 * OL
 * MULTIEDIT
 * LI
 * LEFT
 * INPUT
 * IMG
 * I
 * HTML
 * H7
 * H6
 * H5
 * H4
 * H3
 * H2
 * H1
 * FONT
 * EXTEND
 * EDIT
 * COMMENT
 * COMBOBOX
 * CENTER
 * BUTTON
 * BR
 * BODY
 * BAR
 * ADDRESS
 * A
 * SEL
 * LIST
 * VAR
 * FORE
 * READONL
 * ROWS
 * VALIGN
 * FIXWIDTH
 * BORDERCOLORLI
 * BORDERCOLORDA
 * BORDERCOLOR
 * BORDER
 * BGCOLOR
 * BACKGROUND
 * ALIGN
 * VALU
 * READONLY
 * MULTIPLE
 * SELECTED
 * TYP
 * TYPE
 * MAXLENGTH
 * CHECKED
 * SRC
 * Y
 * X
 * QUERYDELAY
 * NOSCROLLBAR
 * IMGSRC
 * B
 * FG
 * SIZE
 * FACE
 * COLOR
 * DEFFON
 * DEFFIXEDFONT
 * WIDTH
 * VALUE
 * TOOLTIP
 * NAME
 * MIN
 * MAX
 * HEIGHT
 * DISABLED
 * ALIGN
 * MSG
 * LINK
 * HREF
 * ACTION
 */
public class NpcHtmlMessage extends L2GameServerPacket {
    // d S
    // d is usually 0, S is the html text starting with <html> and ending with </html>
    //
    private int _npcObjId;
    private String _html;
    private String _file = null;
    private ArrayList<String> _replaces = new ArrayList<String>();
    private int item_id = 0;
    private boolean have_appends = false;
    private boolean can_writeImpl = false;

    public NpcHtmlMessage(L2Player player, L2NpcInstance npc, String filename, int val) {
        _npcObjId = npc.getObjectId();

        player.setLastNpc(npc);

        ArrayList<ScriptClassAndMethod> appends = Scripts.dialogAppends.get(npc.getNpcId());
        if (appends != null && appends.size() > 0) {
            have_appends = true;
            if (filename != null && filename.equalsIgnoreCase("data/html/npcdefault.htm"))
                setHtml(""); // контент задается скриптами через DialogAppend_
            else
                setFile(filename);

            String replaces = "";

            // Добавить в конец странички текст, определенный в скриптах.
            Object[] script_args = new Object[]{new Integer(val)};
            for (ScriptClassAndMethod append : appends) {
                Object obj = Scripts.callScripts(append.scriptClass, append.method, player, script_args);
                if (obj != null)
                    replaces += obj;
            }

            if (!replaces.equals(""))
                replace("</body>", "\n" + Strings.bbParse(replaces) + "</body>");
        } else
            setFile(filename);

        replace("%npcId%", String.valueOf(npc.getNpcId()));
        replace("%npcname%", npc.getName());
        replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
        replace("%epic_state%", EpicRespawnTimesHolder.getInstance().getStatus(npc.getNpcId()));
    }

    public NpcHtmlMessage(L2Player player, L2NpcInstance npc) {
        _npcObjId = npc.getObjectId();
        player.setLastNpc(npc);
    }

    public NpcHtmlMessage(int npcObjId) {
        _npcObjId = npcObjId;
    }

    public final NpcHtmlMessage setHtml(String text) {
        if (!text.contains("<html>"))
            text = "<html><body>" + text + "</body></html>"; //<title>Message:</title> <br><br><br>
        _html = text;
        return this;
    }

    public final NpcHtmlMessage setFile(String file) {
        _file = file;
        return this;
    }

    public final NpcHtmlMessage setItemId(int _item_id) {
        item_id = _item_id;
        return this;
    }

    private void setFile() {
        String content = loadHtml(_file, getClient().getActiveChar());
        if (content == null)
            setHtml(have_appends && _file.endsWith(".htm") ? "" : _file);
        else
            setHtml(content);
    }

    protected String loadHtml(String name, L2Player player) {
        return Files.read(name, player);
    }

    protected String html_load(String name, String lang) {
        String content = Files.read(name, lang);
        if (content == null)
            content = "Can't find file'" + name + "'";
        return content;
    }

    public void replace(String pattern, String value) {
        _replaces.add(pattern);
        _replaces.add(value);
    }

    @Override
    final public void runImpl() {
        if (_file != null)
            setFile();
        L2Player player = getClient().getActiveChar();
        if (player == null)
            return;

        for (int i = 0; i < _replaces.size(); i += 2)
            _html = _html.replaceAll(_replaces.get(i), _replaces.get(i + 1));
        _html = _html.replaceAll("%objectId%", String.valueOf(_npcObjId));

        player.cleanBypasses(false);
        _html = player.encodeBypasses(_html, false);
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl)
            return;
        writeC(0x0f);
        writeD(_npcObjId);
        writeS(_html);
        writeD(item_id);
    }
}