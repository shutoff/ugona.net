package net.ugona.plus;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class Config {

    protected boolean upd;

    static public boolean update(Object o, JsonObject from) {
        Field[] fields = o.getClass().getDeclaredFields();
        boolean upd = false;
        try {
            for (Field f : fields) {
                String name = f.getName();
                if (name.equals("upd"))
                    continue;
                JsonValue v = from.get(name);
                if (v == null)
                    continue;
                f.setAccessible(true);
                Class<?> c = f.getType();
                if (c.isArray()) {
                    JsonArray arr = v.asArray();
                    Class<?> cel = c.getComponentType();
                    Object av = f.get(o);
                    if ((av == null) || (Array.getLength(av) != arr.size())) {
                        av = Array.newInstance(cel, arr.size());
                        f.set(o, av);
                        upd = true;
                    }
                    for (int i = 0; i < arr.size(); i++) {
                        Object el = Array.get(av, i);
                        if (el == null) {
                            el = cel.newInstance();
                            Array.set(av, i, el);
                        }
                        if (update(el, arr.get(i).asObject()))
                            upd = true;
                    }
                    continue;
                }
                Type t = f.getGenericType();
                if ((t == int.class) && v.isNumber()) {
                    int iv = v.asInt();
                    if (iv != f.getInt(o)) {
                        f.setInt(o, iv);
                        upd = true;
                    }
                } else if ((t == long.class) && v.isNumber()) {
                    long lv = v.asLong();
                    if (lv != f.getLong(o)) {
                        f.setLong(o, lv);
                        upd = true;
                    }
                } else if ((t == double.class) && v.isNumber()) {
                    double dv = v.asDouble();
                    if (dv != f.getDouble(o)) {
                        f.setDouble(o, dv);
                        upd = true;
                    }
                } else if ((t == boolean.class) && v.isBoolean()) {
                    boolean bv = v.asBoolean();
                    if (bv != f.getBoolean(o)) {
                        f.setBoolean(o, bv);
                        upd = true;
                    }
                } else if ((t == String.class) && v.isString()) {
                    String sv = v.asString();
                    if (!sv.equals(f.get(o))) {
                        f.set(o, sv);
                        upd = true;
                    }
                } else if ((t == Integer.class) && v.isNumber()) {
                    int iv = v.asInt();
                    Integer ov = (Integer) f.get(o);
                    if ((ov == null) || (ov != iv)) {
                        f.set(o, (Integer) iv);
                        upd = true;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (upd && (o instanceof Config)) {
            Config c = (Config) o;
            c.upd = true;
        }
        return upd;
    }

    static public JsonObject saveJson(Object o) {
        if (o instanceof JsonObject)
            return (JsonObject) o;
        Field[] fields = o.getClass().getDeclaredFields();
        JsonObject res = new JsonObject();
        try {
            for (Field f : fields) {
                String name = f.getName();
                if (name.equals("upd"))
                    continue;
                f.setAccessible(true);
                Class<?> c = f.getType();
                if (c.isArray()) {
                    Object v = f.get(o);
                    if (v == null)
                        continue;
                    int length = Array.getLength(v);
                    JsonArray arr = new JsonArray();
                    for (int i = 0; i < length; i++) {
                        Object el = Array.get(v, i);
                        if (el == null)
                            continue;
                        arr.add(saveJson(el));
                    }
                    res.add(name, arr);
                    continue;
                }
                Type t = f.getGenericType();
                if (t == int.class) {
                    res.add(name, f.getInt(o));
                } else if (t == long.class) {
                    res.add(name, f.getLong(o));
                } else if (t == double.class) {
                    res.add(name, f.getDouble(o));
                } else if (t == boolean.class) {
                    res.add(name, f.getBoolean(o));
                } else if (t == String.class) {
                    Object v = f.get(o);
                    if (v != null)
                        res.add(name, v.toString());
                } else if (t == Integer.class) {
                    Object v = f.get(o);
                    if (v != null)
                        res.add(name, (Integer) v);
                }
            }
        } catch (IllegalAccessException ex) {
            // ignore
        }
        if (o instanceof Config) {
            Config c = (Config) o;
            c.upd = false;
        }
        return res;
    }

    static public String save(Object o) {
        JsonObject res = saveJson(o);
        return res.toString();
    }

}
