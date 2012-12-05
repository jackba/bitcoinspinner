package com.miracleas.bitcoin_spinner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;

public class AddressBookManager {
  private static AddressBookManager _instance;
  private static final String ADDRESS_BOOK_FILE_NAME = "address-book.txt";

  public static class Entry implements Comparable<Entry> {
    private String _address;
    private String _name;

    public Entry(String address, String name) {
      _address = address;
      _name = name == null ? "" : name;
    }

    public String getAddress() {
      return _address;
    }

    public String getName() {
      return _name;
    }

    @Override
    public int compareTo(Entry another) {
      return _name.compareToIgnoreCase(another._name);
    }

    @Override
    public int hashCode() {
      return _name.hashCode() + _address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Entry)) {
        return false;
      }
      Entry other = (Entry) obj;
      return _address.equals(other._address) && _name.equals(other._name);
    }

  }

  private List<Entry> _entries;
  private Map<String, Entry> _addressMap;
  private Map<String, Entry> _nameMap;

  private AddressBookManager() {
    List<Entry> entries = loadEntries();
    _entries = new ArrayList<Entry>(entries.size());
    _addressMap = new HashMap<String, Entry>(entries.size());
    _nameMap = new HashMap<String, Entry>(entries.size());
    for (Entry entry : entries) {
      addEntryInt(entry.getAddress(), entry.getName());
    }
    Collections.sort(_entries);
  }

  public synchronized void addEntry(String address, String name) {
    if (address == null || name == null) {
      return;
    }
    addEntryInt(address, name);
    Collections.sort(_entries);
    save();
  }

  public synchronized void deleteEntry(String address) {
    if (address == null) {
      return;
    }
    address = address.trim();
    Entry entry = _addressMap.get(address);
    if (entry == null) {
      return;
    }
    _entries.remove(entry);
    _addressMap.remove(address);
    _nameMap.remove(entry.getName());
    save();
  }

  private void addEntryInt(String address, String name) {
    if (address == null) {
      return;
    }
    address = address.trim();
    if (address.length() == 0) {
      return;
    }
    if (name == null) {
      name = "";
    }
    name = name.trim();

    Entry entry = _addressMap.get(address);
    if (entry == null) {
      entry = new Entry(address, name);
      _entries.add(new Entry(address, name));
    } else {
      _entries.remove(entry);
      entry._name = name;
      _entries.add(entry);
    }
    _addressMap.put(address, entry);
    if (name.length() != 0) {
      _nameMap.put(name, entry);
    }
  }

  public static synchronized AddressBookManager getInstance() {
    if (_instance == null) {
      _instance = new AddressBookManager();
    }
    return _instance;
  }

  public String getAddressByName(String name) {
    if (name == null) {
      return null;
    }
    return _nameMap.get(name.trim()).getAddress();
  }

  public boolean hasAddress(String address) {
    return _addressMap.containsKey(address);
  }

  public String getNameByAddress(String address) {
    if (address == null) {
      return null;
    }
    Entry entry = _addressMap.get(address.trim());
    if (entry == null) {
      return "";
    }
    return entry.getName();
  }

  public List<Entry> getEntries() {
    return Collections.unmodifiableList(_entries);
  }

  public int numEntries() {
    return _entries.size();
  }

  private void save() {
    saveEntries(_entries);
  }

  private static void saveEntries(List<Entry> entries) {
    try {
      Context context = SpinnerContext.getInstance().getApplicationContext();
      FileOutputStream out = context.openFileOutput(ADDRESS_BOOK_FILE_NAME, Context.MODE_PRIVATE);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
      for (Entry entry : entries) {
        StringBuilder sb = new StringBuilder();
        sb.append(encode(entry.getAddress())).append(',').append(encode(entry._name));
        sb.append('\n');
        writer.write(sb.toString());
      }
      writer.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  private static List<Entry> loadEntries() {
    try {
      List<Entry> entries = new ArrayList<Entry>();
      BufferedReader stream;
      try {
        Context context = SpinnerContext.getInstance().getApplicationContext();
        stream = new BufferedReader(new InputStreamReader(context.openFileInput(ADDRESS_BOOK_FILE_NAME)));
      } catch (FileNotFoundException e) {
        // ignore and return an empty set of addresses
        return entries;
      }

      while (true) {
        String line = stream.readLine();
        if (line == null) {
          break;
        }
        List<String> list = stringToValueList(line);
        String address = null;
        if (list.size() > 0) {
          address = decode(list.get(0));
        }
        String name = null;
        if (list.size() > 1) {
          name = decode(list.get(1));
        }
        entries.add(new Entry(address, name));
      }
      stream.close();
      return entries;
    } catch (Exception e) {
      e.printStackTrace();
      // ignore and return an empty set of addresses
      return new ArrayList<Entry>();
    }
  }

  private static List<String> stringToValueList(String string) {
    int startIndex = 0;
    List<String> values = new LinkedList<String>();
    while (true) {
      int separatorIndex = nextSeparator(string, startIndex);
      if (separatorIndex == -1) {
        // something wrong, return empty list
        return new LinkedList<String>();
      }
      String value = string.substring(startIndex, separatorIndex);
      startIndex = separatorIndex + 1;
      values.add(value);
      if (separatorIndex == string.length()) {
        break;
      }
    }
    return values;
  }

  /**
   * Find the next ',' occurrence in a string where: we skip '/' followed by any
   * char, skip anything in an opening parenthesis until we hit a matching
   * closing parenthesis
   * 
   * @param s
   *          the string to find the next ',' in
   * @param startIndex
   *          the start index where the search starts
   * @return the resulting comma index or the length of the string
   * @throws PersistenceException
   */
  private static int nextSeparator(String s, int startIndex) {
    boolean slash = false;
    int pCounter = 0;
    for (int i = startIndex; i < s.length(); i++) {
      if (slash) {
        slash = false;
        continue;
      }
      char c = s.charAt(i);
      if (c == '/') {
        slash = true;
        continue;
      }
      if (c == '(') {
        pCounter++;
        continue;
      }
      if (c == ')') {
        if (pCounter < 0) {
          return -1;
        }
        pCounter--;
        continue;
      }
      if (pCounter > 0) {
        continue;
      }
      if (c == ',') {
        return i;
      }
    }
    if (pCounter < 0) {
      return -1;
    }
    return s.length();
  }

  private static String encode(String value) {
    if (value == null) {
      // Treat null string values as the empty string
      value = "";
    }
    if (value.indexOf('/') == -1 && value.indexOf(',') == -1) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length() + 1);
    char[] chars = value.toCharArray();
    for (char c : chars) {
      if (c == '/') {
        sb.append("//");
      } else if (c == ',') {
        sb.append("/,");
      } else if (c == '(') {
        sb.append("/(");
      } else if (c == ')') {
        sb.append("/)");
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String decode(String value) {
    StringBuilder sb = new StringBuilder(value.length() + 1);
    char[] chars = value.toCharArray();
    boolean slash = false;
    for (char c : chars) {
      if (slash) {
        slash = false;
        if (c == '/') {
          sb.append('/');
        } else if (c == ',') {
          sb.append(',');
        } else if (c == '(') {
          sb.append('(');
        } else if (c == ')') {
          sb.append(')');
        } else {
          // decode error, ignore this character
        }
      } else {
        if (c == '/') {
          slash = true;
        } else {
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }

}
