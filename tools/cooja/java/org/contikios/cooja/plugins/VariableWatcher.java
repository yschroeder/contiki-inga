/*
 * Copyright (c) 2009, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */
package org.contikios.cooja.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import org.apache.log4j.Logger;

import org.jdom.Element;

import org.contikios.cooja.ClassDescription;
import org.contikios.cooja.Cooja;
import org.contikios.cooja.Mote;
import org.contikios.cooja.MotePlugin;
import org.contikios.cooja.PluginType;
import org.contikios.cooja.Simulation;
import org.contikios.cooja.VisPlugin;
import org.contikios.cooja.mote.memory.UnknownVariableException;
import org.contikios.cooja.mote.memory.VarMemory;
import org.contikios.cooja.mote.memory.Memory;
import org.contikios.cooja.mote.memory.Memory.MemoryMonitor;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 * Variable Watcher enables a user to watch mote variables during a simulation.
 * Variables can be read or written either as bytes, integers or byte arrays.
 *
 * User can also see which variables seems to be available on the selected node.
 *
 * @author Fredrik Osterlind
 */
@ClassDescription("Variable Watcher")
@PluginType(PluginType.MOTE_PLUGIN)
public class VariableWatcher extends VisPlugin implements MotePlugin {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(VariableWatcher.class.getName());

  private final static int LABEL_WIDTH = 120;
  private final static int LABEL_HEIGHT = 15;

  private JComboBox varNameCombo;
  private JComboBox varTypeCombo;
  private JComboBox varFormatCombo;
  private JPanel infoPane;
  private JTextField varAddressField;
  private JTextField varSizeField;
  private JPanel valuePane;
  private JFormattedTextField[] varValues;
  private byte[] bufferedBytes;
  private JButton readButton;
  private AbstractButton monitorButton;
  private JButton writeButton;
  private JLabel debuglbl;
  private KeyListener charValueKeyListener;
  private FocusListener charValueFocusListener;
  private VarMemory moteMemory;

  MemoryMonitor memMonitor;
  long monitorAddr;
  int monitorSize;

  private NumberFormat integerFormat;
  private ValueFormatter hf;

  private Mote mote;

  public enum VarTypes {

    BYTE("byte", 1),
    SHORT("short", 2),
    INT("int", 2),
    LONG("long", 4),
    ADDR("address", 4);

    String mRep;
    int mSize;

    VarTypes(String rep, int size) {
      mRep = rep;
      mSize = size;
    }

    /**
     * Returns the number of bytes for this data type.
     *
     * @return Size in bytes
     */
    public int getBytes() {
      return mSize;
    }

    protected void setBytes(int size) {
      mSize = size;
    }

    /**
     * Returns String name of this variable type.
     *
     * @return Type name
     */
    @Override
    public String toString() {
      return mRep;
    }
  }

  public enum VarFormats {

    CHAR("Char", 0),
    DEC("Decimal", 10),
    HEX("Hex", 16);

    String mRep;
    int mBase;

    VarFormats(String rep, int base) {
      mRep = rep;
      mBase = base;
    }

    /**
     * Returns String name of this variable representation.
     *
     * @return Type name
     */
    @Override
    public String toString() {
      return mRep;
    }
  }

  VarFormats[] valueFormats = {VarFormats.CHAR, VarFormats.DEC, VarFormats.HEX};
  VarTypes[] valueTypes = {VarTypes.BYTE, VarTypes.SHORT, VarTypes.INT, VarTypes.LONG, VarTypes.ADDR};

  /**
   * @param moteToView Mote
   * @param simulation Simulation
   * @param gui GUI
   */
  public VariableWatcher(Mote moteToView, Simulation simulation, Cooja gui) {
    super("Variable Watcher (" + moteToView + ")", gui);
    this.mote = moteToView;
    moteMemory = (VarMemory) moteToView.getMemory();

    JLabel label;
    integerFormat = NumberFormat.getIntegerInstance();
    JPanel mainPane = new JPanel();
    mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
    JPanel smallPane;
    JPanel readPane;

    // Variable name
    smallPane = new JPanel(new BorderLayout());
    label = new JLabel("Variable name");
    label.setPreferredSize(new Dimension(LABEL_WIDTH, LABEL_HEIGHT));
    smallPane.add(BorderLayout.WEST, label);

    String[] allPotentialVarNames = moteMemory.getVariableNames();
    Arrays.sort(allPotentialVarNames);

    varNameCombo = new JComboBox(allPotentialVarNames);
    AutoCompleteDecorator.decorate(varNameCombo);
    varNameCombo.setEditable(true);
    varNameCombo.setSelectedItem("");
 
    // use longest variable name as prototye for width
    String longestVarname = "";
    int maxLength = 0;
    for (String w : allPotentialVarNames) {
      if (w.length() > maxLength) {
        maxLength = w.length();
        longestVarname = w;
      }
    }
    varNameCombo.setPrototypeDisplayValue(longestVarname);

    varNameCombo.addPopupMenuListener(new PopupMenuListener() {
      
      String lastItem = "";

      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      // apply new variable name if popup is closed
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        String currentItem = (String)varNameCombo.getSelectedItem();
        
        /* If item did not changed, skip! */
        if (currentItem == null || currentItem.equals(lastItem)) {
          return;
        }
        lastItem = currentItem;

        try {
          /* invalidate byte field */
          bufferedBytes = null;
          /* calculate number of elements required to show the value in the given size */
          updateNumberOfValues();
          varAddressField.setText(String.format("0x%04x", moteMemory.getVariableAddress(currentItem)));
          long size = moteMemory.getVariableSize(currentItem);
          /* Disable buttons if variable reported size < 1, activate otherwise */
          if (size < 1) {
            varSizeField.setText("N/A");
            readButton.setEnabled(false);
            monitorButton.setEnabled(false);
            writeButton.setEnabled(false);
          } else {
            varSizeField.setText(String.valueOf(size));
            readButton.setEnabled(true);
            monitorButton.setEnabled(true);
            writeButton.setEnabled(true);
          }
        }
        catch (UnknownVariableException ex) {
          ((JTextField) varNameCombo.getEditor().getEditorComponent()).setForeground(Color.RED);
          writeButton.setEnabled(false);
        }
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });

    smallPane.add(BorderLayout.EAST, varNameCombo);
    mainPane.add(smallPane);

    // Variable type
    smallPane = new JPanel(new BorderLayout());
    label = new JLabel("Variable type");
    label.setPreferredSize(new Dimension(LABEL_WIDTH, LABEL_HEIGHT));
    smallPane.add(BorderLayout.WEST, label);

    /* set correct integer and address size */
    valueTypes[2].setBytes(moteMemory.getMemoryLayout().intSize);
    valueTypes[4].setBytes(moteMemory.getMemoryLayout().addrSize);

    JPanel reprPanel = new JPanel(new BorderLayout());
    varTypeCombo = new JComboBox(valueTypes);
    varTypeCombo.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          hf.setType((VarTypes) e.getItem());
          updateNumberOfValues(); // number of elements should have changed
        }
      }
    });

    varFormatCombo = new JComboBox(valueFormats);
    varFormatCombo.setSelectedItem(VarFormats.HEX);
    varFormatCombo.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {

          hf.setFormat((VarFormats) e.getItem());
          refreshValues(); // format of elements should have changed
        }
      }
    });

    reprPanel.add(BorderLayout.WEST, varTypeCombo);
    reprPanel.add(BorderLayout.EAST, varFormatCombo);

    smallPane.add(BorderLayout.EAST, reprPanel);
    mainPane.add(smallPane);

    /* The recommended fix for the bug #4740914
     * Synopsis : Doing selectAll() in a JFormattedTextField on focusGained
     * event doesn't work. 
     */
//    jFormattedTextFocusAdapter = new FocusAdapter() {
//      public void focusGained(final FocusEvent ev) {
//        SwingUtilities.invokeLater(new Runnable() {
//          public void run() {
//            JTextField jtxt = (JTextField)ev.getSource();
//            jtxt.selectAll();
//          }
//        });
//      }
//    };
    mainPane.add(Box.createRigidArea(new Dimension(0, 5)));

    infoPane = new JPanel();
    infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.Y_AXIS));

    JPanel addrInfoPane = new JPanel(new BorderLayout());
    JPanel sizeInfoPane = new JPanel(new BorderLayout());

    label = new JLabel("Address");
    addrInfoPane.add(BorderLayout.CENTER, label);
    varAddressField = new JTextField("?");
    varAddressField.setEditable(false);
    varAddressField.setEnabled(false);
    varAddressField.setColumns(6);
    addrInfoPane.add(BorderLayout.EAST, varAddressField);

    infoPane.add(addrInfoPane);

    label = new JLabel("Size");
    sizeInfoPane.add(BorderLayout.WEST, label);
    varSizeField = new JTextField("?");
    varSizeField.setEditable(false);
    varSizeField.setEnabled(false);
    varSizeField.setColumns(6);
    sizeInfoPane.add(BorderLayout.EAST, varSizeField);

    infoPane.add(sizeInfoPane);
    mainPane.add(infoPane);

    // Variable value label
    label = new JLabel("Variable value");
    label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    label.setPreferredSize(new Dimension(LABEL_WIDTH, LABEL_HEIGHT));
    mainPane.add(label);

    // Variable value(s)
    valuePane = new JPanel();
    valuePane.setLayout(new BoxLayout(valuePane, BoxLayout.Y_AXIS));

    hf = new ValueFormatter(
            (VarTypes) varTypeCombo.getSelectedItem(),
            (VarFormats) varFormatCombo.getSelectedItem());

    varValues = new JFormattedTextField[1];
    varValues[0] = new JFormattedTextField(integerFormat);
    varValues[0].setValue(new Integer(0));
    varValues[0].setColumns(3);
    varValues[0].setText("?");

    mainPane.add(valuePane);
    mainPane.add(Box.createRigidArea(new Dimension(0, 5)));

    debuglbl = new JLabel();
    mainPane.add(new JPanel().add(debuglbl));
    mainPane.add(Box.createRigidArea(new Dimension(0, 5)));

    smallPane = new JPanel(new BorderLayout());
    readPane = new JPanel(new BorderLayout());

    /* Read button */
    readButton = new JButton("Read");
    readButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (!moteMemory.variableExists((String) varNameCombo.getSelectedItem())) {
          ((JTextField) varNameCombo.getEditor().getEditorComponent()).setForeground(Color.RED);
          writeButton.setEnabled(false);
          return;
        }

        writeButton.setEnabled(true);
        bufferedBytes = moteMemory.getByteArray(
                (String) varNameCombo.getSelectedItem(),
                moteMemory.getVariableSize((String) varNameCombo.getSelectedItem()));
        refreshValues();
      }
    });

    readPane.add(BorderLayout.WEST, readButton);

    /* MemoryMonitor required for monitor button */

    /* Monitor button */
    monitorButton = new JToggleButton("Monitor");
    monitorButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        String varname = (String) varNameCombo.getSelectedItem();

        // deselect
        if (!((JToggleButton) e.getSource()).isSelected()) {
          //System.out.println("Removing monitor " + memMonitor + " for addr " + monitorAddr + ", size " + monitorSize + "");
          moteMemory.removeMemoryMonitor(monitorAddr, monitorSize, memMonitor);
          readButton.setEnabled(true);
          varNameCombo.setEnabled(true);
          writeButton.setEnabled(true);
          return;
        }

        // handle unknown variable
        if (!moteMemory.variableExists(varname)) {
          ((JTextField) varNameCombo.getEditor().getEditorComponent()).setForeground(Color.RED);
          writeButton.setEnabled(false);
          ((JToggleButton) e.getSource()).setSelected(false);
          return;
        }

        /* initial readout so we have a value to display */
        bufferedBytes = moteMemory.getByteArray(
                (String) varNameCombo.getSelectedItem(),
                moteMemory.getVariableSize((String) varNameCombo.getSelectedItem()));
        refreshValues();

        monitorAddr = moteMemory.getVariableAddress(varname);
        monitorSize = moteMemory.getVariableSize(varname);
        memMonitor = new MemoryMonitor() {

          @Override
          public void memoryChanged(Memory memory, EventType type, long address) {
            bufferedBytes = moteMemory.getByteArray(
                    (String) varNameCombo.getSelectedItem(),
                    moteMemory.getVariableSize((String) varNameCombo.getSelectedItem()));

            refreshValues();
          }
        };
        //System.out.println("Adding monitor " + memMonitor + " for addr " + monitorAddr + ", size " + monitorSize + "");
        moteMemory.addMemoryMonitor(
                MemoryMonitor.EventType.WRITE,
                monitorAddr,
                monitorSize,
                memMonitor);

        /* During monitoring we neither allow writes nor to change variable */
        readButton.setEnabled(false);
        writeButton.setEnabled(false);
        varNameCombo.setEnabled(false);
      }
    });

    readPane.add(BorderLayout.EAST, monitorButton);
    smallPane.add(BorderLayout.WEST, readPane);

    /* Write button */
    writeButton = new JButton("Write");
    writeButton.setEnabled(false);
    writeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        /* Convert from input field to byte array */
        int bytes = ((VarTypes) varTypeCombo.getSelectedItem()).getBytes();
        for (int element = 0; element < varValues.length; element++) {
          for (int b = 0; b < bytes; b++) {
            if (element * bytes + b > bufferedBytes.length - 1) {
              break;
            }
            bufferedBytes[element * bytes + b] = (byte) ((((int) varValues[element].getValue()) >> (b * 8)) & 0xFF);
          }
        }
        /* Write to memory */
        moteMemory.setMemorySegment(
                moteMemory.getVariableAddress((String) varNameCombo.getSelectedItem()),
                bufferedBytes);
      }
    });

    smallPane.add(BorderLayout.EAST, writeButton);
    mainPane.add(smallPane);

    add(BorderLayout.NORTH, mainPane);
  }

  @Override
  public void packPlugin(JDesktopPane pane) {
    pack();
  }
  
  /**
   * Simple String to Hex to String conversion.
   */
  public class ValueFormatter extends JFormattedTextField.AbstractFormatter {

    final String TEXT_NOT_TO_TOUCH;

    private VarTypes mType;
    private VarFormats mFormat;

    public ValueFormatter(VarTypes type, VarFormats format) {
      mType = type;
      mFormat = format;
      if (mFormat == VarFormats.HEX) {
        TEXT_NOT_TO_TOUCH = "0x";
      }
      else {
        TEXT_NOT_TO_TOUCH = "";
      }
    }

    public void setType(VarTypes type) {
      mType = type;
    }

    public void setFormat(VarFormats format) {
      mFormat = format;
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
//      System.out.println("HexFormatter stringToValue('" + text + "')");
      Object ret;
      switch (mFormat) {
        case CHAR:
          ret = text.charAt(0);
          break;
        case DEC:
        case HEX:
          try {
            ret = Integer.decode(text);
          }
          catch (NumberFormatException ex) {
            ret = 0;
          }
          break;
        default:
          ret = null;
      }
      return ret;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
//      System.out.println("ValueFormatter valueToStrign(" + value + ")");
      if (value == null) {
        return "?";
      }

      switch (mFormat) {
        case CHAR:
          return String.format("%c", value);
        case DEC:
          return String.format("%d", value);
        case HEX:
          return String.format("0x%x", value);
        default:
          return "";
      }
    }

    /* Do not override TEXT_NOT_TO_TOUCH */
    @Override
    public DocumentFilter getDocumentFilter() {
      /**
       * @todo: There seem to be some remaining issues regarding input handling
       */
      return new DocumentFilter() {

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
          //System.out.println("insertString!");
          if (offset < TEXT_NOT_TO_TOUCH.length()) {
            return;
          }
          super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
          //System.out.println("replace!");
          if (offset < TEXT_NOT_TO_TOUCH.length()) {
            length = Math.max(0, length - TEXT_NOT_TO_TOUCH.length());
            offset = TEXT_NOT_TO_TOUCH.length();
          }
          super.replace(fb, offset, length, text, attrs);
        }

        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
          //System.out.println("remove!");
          if (offset < TEXT_NOT_TO_TOUCH.length()) {
            length = Math.max(0, length + offset - TEXT_NOT_TO_TOUCH.length());
            offset = TEXT_NOT_TO_TOUCH.length();
          }
          if (length > 0) {
            super.remove(fb, offset, length);
          }
        }
      };
    }

  }

  /**
   * Updates all value fields based on buffered data.
   *
   * @note Does not read memory. Leaves number of fields unchanged.
   */
  private void refreshValues() {
    String varname = (String) varNameCombo.getSelectedItem();

    if (!moteMemory.variableExists(varname)) {
      return;
    }

    int bytes = moteMemory.getVariableSize(varname);
    int typeSize = ((VarTypes) varTypeCombo.getSelectedItem()).getBytes();
    int elements = (int) Math.ceil((double) bytes / typeSize);

    /* Skip if we have no data to set */
    if ((bufferedBytes == null) || (bufferedBytes.length < bytes)) {
      return;
    }

    /* Set values based on buffered data */
    for (int i = 0; i < elements; i += 1) {
      int val = 0;
      for (int j = 0; j < typeSize; j++) {
        /* skip if we do note have more bytes do consume.
         This may happen e.g. if we display long (4 bytes) but have only 3 bytes */
        if (i * typeSize + j > bufferedBytes.length - 1) {
          break;
        }
        val += ((bufferedBytes[i * typeSize + j] & 0xFF) << (j * 8));
      }
      varValues[i].setValue(val);
      try {
        varValues[i].commitEdit();
      }
      catch (ParseException ex) {
        logger.error(ex);
      }
    }

  }

  /**
   * Updates the number of value fields.
   */
  private void updateNumberOfValues() {
    String varname = (String) varNameCombo.getSelectedItem();

    if (!moteMemory.variableExists(varname)) {
      return;
    }

    valuePane.removeAll();

    JPanel linePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    DefaultFormatterFactory defac = new DefaultFormatterFactory(hf);

    long address = moteMemory.getVariableAddress((String) varNameCombo.getSelectedItem());
    int bytes = moteMemory.getVariableSize((String) varNameCombo.getSelectedItem());
    int typeSize = ((VarTypes) varTypeCombo.getSelectedItem()).getBytes();
    int elements = (int) Math.ceil((double) bytes / typeSize);

    if (elements > 0) {
      varValues = new JFormattedTextField[elements];
      for (int i = 0; i < elements; i++) {
        varValues[i] = new JFormattedTextField(defac);
        varValues[i].setColumns(6);
        varValues[i].setToolTipText(String.format("0x%04x", address + i * typeSize));
        linePane.add(varValues[i]);
        /* After 8 Elemens, break line */
        if ((i + 1) % 8 == 0) {
          valuePane.add(linePane);
          linePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }
      }
      valuePane.add(linePane);
    }

    refreshValues();

    pack();

    /* Assure we do not loose the window due to placement outside desktop */
    if (getX() < 0) {
      setLocation(0, getY());
    }
  }

  @Override
  public void closePlugin() {
    /* Make sure to release monitor */
    if (monitorButton.isSelected()) {
      monitorButton.doClick();
    }
  }

  @Override
  public Collection<Element> getConfigXML() {
    // Return currently watched variable and type
    List<Element> config = new LinkedList<>();

    Element element;

    // Selected variable name
    element = new Element("varname");
    element.setText((String) varNameCombo.getSelectedItem());
    config.add(element);

    // Selected variable type
    element = new Element("vartype");
    element.setText(String.valueOf(varTypeCombo.getSelectedIndex()));
    config.add(element);

    // Selected output format
    element = new Element("varformat");
    element.setText(String.valueOf(varFormatCombo.getSelectedIndex()));
    config.add(element);
    return config;
  }

  @Override
  public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    for (Element element : configXML) {
      switch (element.getName()) {
        case "varname":
          varNameCombo.setSelectedItem(element.getText());
          break;
        case "vartype":
          varTypeCombo.setSelectedIndex(Integer.parseInt(element.getText()));
          break;
        case "varformat":
          varFormatCombo.setSelectedIndex(Integer.parseInt(element.getText()));
          break;
      }
    }
    updateNumberOfValues();

    return true;
  }

  @Override
  public Mote getMote() {
    return mote;
  }
}

/* Limit JTextField input class */
class JTextFieldLimit extends PlainDocument {

  private static final long serialVersionUID = 1L;
  private int limit;
  // optional uppercase conversion
  private boolean toUppercase = false;

  JTextFieldLimit(int limit) {
    super();
    this.limit = limit;
  }

  JTextFieldLimit(int limit, boolean upper) {
    super();
    this.limit = limit;
    toUppercase = upper;
  }

  @Override
  public void insertString(int offset, String str, AttributeSet attr)
          throws BadLocationException {
    if (str == null) {
      return;
    }

    if ((getLength() + str.length()) <= limit) {
      if (toUppercase) {
        str = str.toUpperCase();
      }
      super.insertString(offset, str, attr);
    }
  }
}
