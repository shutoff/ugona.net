package net.ugona.plus;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

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
                Type t = f.getGenericType();
                if (t == int.class) {
                    int iv = v.asInt();
                    if (iv != f.getInt(o)) {
                        f.setInt(o, iv);
                        upd = true;
                    }
                } else if (t == long.class) {
                    long lv = v.asLong();
                    if (lv != f.getLong(o)) {
                        f.setLong(o, lv);
                        upd = true;
                    }
                } else if (t == double.class) {
                    double dv = v.asDouble();
                    if (dv != f.getDouble(o)) {
                        f.setDouble(o, dv);
                        upd = true;
                    }
                } else if (t == boolean.class) {
                    boolean bv = v.asBoolean();
                    if (bv != f.getBoolean(o)) {
                        f.setBoolean(o, bv);
                        upd = true;
                    }
                } else if (t == String.class) {
                    String sv = v.asString();
                    if (!sv.equals(f.get(o))) {
                        f.set(o, sv);
                        upd = true;
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            // ignore
        }
        if (upd && (o instanceof Config)) {
            Config c = (Config) o;
            c.upd = true;
        }
        return upd;
    }

    static public String save(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        JsonObject res = new JsonObject();
        try {
            for (Field f : fields) {
                String name = f.getName();
                if (name.equals("upd"))
                    continue;
                f.setAccessible(true);
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
                }
            }
        } catch (IllegalAccessException ex) {
            // ignore
        }
        if (o instanceof Config) {
            Config c = (Config) o;
            c.upd = false;
        }
        return res.toString();
    }

}
