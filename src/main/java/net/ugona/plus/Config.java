package net.ugona.plus;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class Config {

    protected boolean upd;

    static public void clear(Object o) {
        synchronized (o) {
            Field[] fields = o.getClass().getDeclaredFields();
            try {
                for (Field f : fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0)
                        continue;
                    String name = f.getName();
                    if (name.equals("upd"))
                        continue;
                    f.setAccessible(true);
                    Class<?> c = f.getType();
                    if (c.isArray()) {
                        f.set(o, null);
                        continue;
                    }
                    Type t = f.getGenericType();
                    if (t == int.class) {
                        f.setInt(o, 0);
                    } else if (t == long.class) {
                        f.setLong(o, 0);
                    } else if (t == double.class) {
                        f.setDouble(o, 0);
                    } else if (t == boolean.class) {
                        f.setBoolean(o, false);
                    } else if (t == String.class) {
                        f.set(o, "");
                    }
                }
                Method init = o.getClass().getMethod("invoke");
                if (init != null)
                    init.invoke(o);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    static public Set<String> update(Object o, JsonObject from) {
        Set<String> res = new HashSet<>();
        synchronized (o) {
            Field[] fields = o.getClass().getDeclaredFields();
            try {
                for (Field f : fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0)
                        continue;
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
                        boolean bSet = false;
                        boolean bChanged = false;
                        if ((av == null) || (Array.getLength(av) != arr.size())) {
                            av = Array.newInstance(cel, arr.size());
                            bSet = true;
                            bChanged = true;
                        }
                        for (int i = 0; i < arr.size(); i++) {
                            if (cel == int.class) {
                                if (!arr.get(i).isNumber())
                                    continue;
                                Array.setInt(av, i, arr.get(i).asInt());
                                continue;
                            }
                            if (cel.isPrimitive())
                                continue;
                            Object el = Array.get(av, i);
                            if (el == null) {
                                el = cel.newInstance();
                                Array.set(av, i, el);
                            }
                            if (update(el, arr.get(i).asObject()) != null)
                                bChanged = true;
                        }
                        if (bSet)
                            f.set(o, av);
                        if (bChanged)
                            res.add(name);
                        continue;
                    }
                    Type t = f.getGenericType();
                    if ((t == int.class) && v.isNumber()) {
                        int iv = v.asInt();
                        if (iv != f.getInt(o)) {
                            f.setInt(o, iv);
                            res.add(name);
                        }
                    } else if ((t == long.class) && v.isNumber()) {
                        long lv = v.asLong();
                        if (lv != f.getLong(o)) {
                            f.setLong(o, lv);
                            res.add(name);
                        }
                    } else if ((t == double.class) && v.isNumber()) {
                        double dv = v.asDouble();
                        if (dv != f.getDouble(o)) {
                            f.setDouble(o, dv);
                            res.add(name);
                        }
                    } else if ((t == boolean.class) && v.isBoolean()) {
                        boolean bv = v.asBoolean();
                        if (bv != f.getBoolean(o)) {
                            f.setBoolean(o, bv);
                            res.add(name);
                        }
                    } else if ((t == String.class) && v.isString()) {
                        String sv = v.asString();
                        if (!sv.equals(f.get(o))) {
                            f.set(o, sv);
                            res.add(name);
                        }
                    } else if ((t == Integer.class) && v.isNumber()) {
                        int iv = v.asInt();
                        Integer ov = (Integer) f.get(o);
                        if ((ov == null) || (ov != iv)) {
                            f.set(o, (Integer) iv);
                            res.add(name);
                        }
                    } else if ((t == Long.class) && v.isNumber()) {
                        long iv = v.asLong();
                        Long ov = (Long) f.get(o);
                        if ((ov == null) || (ov != iv)) {
                            f.set(o, (Long) iv);
                            res.add(name);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (res.isEmpty())
            res = null;
        if ((res != null) && (o instanceof Config)) {
            Config c = (Config) o;
            c.upd = true;
        }
        return res;
    }

    static private JsonObject saveJson(Object o) {
        if (o instanceof JsonObject)
            return (JsonObject) o;
        Field[] fields = o.getClass().getDeclaredFields();
        JsonObject res = new JsonObject();
        try {
            for (Field f : fields) {
                if ((f.getModifiers() & Modifier.STATIC) != 0)
                    continue;
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
                    Class<?> cel = c.getComponentType();
                    for (int i = 0; i < length; i++) {
                        if (cel == int.class) {
                            arr.add(Array.getInt(v, i));
                            continue;
                        }
                        if (cel.isPrimitive())
                            continue;
                        Object el = Array.get(v, i);
                        if (el == null)
                            continue;
                        JsonObject r = saveJson(el);
                        if (r != null)
                            arr.add(r);
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
                } else if (t == Long.class) {
                    Object v = f.get(o);
                    if (v != null)
                        res.add(name, (Long) v);
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
        JsonObject res = null;
        synchronized (o) {
            res = saveJson(o);
        }
        return res.toString();
    }

}
