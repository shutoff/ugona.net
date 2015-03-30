package net.ugona.plus;

import android.content.Context;
import android.content.Intent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commands {

    final static Map<String, Queue> requests = new HashMap<>();

    static boolean isProcessed(String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return false;
            Queue queue = requests.get(id);
            return queue.containsKey(cmd);
        }
    }

    static boolean haveProcessed(Context context, String id) {
        boolean bChanged = false;
        boolean bRes = false;
        synchronized (requests) {
            if (!requests.containsKey(id))
                return false;
            Queue queue = requests.get(id);
            long now = new Date().getTime();
            Set<Map.Entry<CarConfig.Command, Long>> entries = queue.entrySet();
            for (Map.Entry<CarConfig.Command, Long> entry : entries) {
                if (entry.getValue() + 900000 < now) {
                    bChanged = true;
                    queue.remove(entry.getKey());
                    continue;
                }
                if (entry.getKey().done != null)
                    bRes = true;
            }
        }
        if (bChanged) {
            Intent i = new Intent(Names.COMMANDS);
            i.putExtra(Names.ID, id);
            context.sendBroadcast(i);
        }
        return bRes;
    }

    static boolean cancel(Context context, String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return false;
            Queue queue = requests.get(id);
            if (!queue.containsKey(cmd))
                return false;
            queue.remove(cmd);
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
        return true;
    }

    static void put(Context context, String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                requests.put(id, new Queue());
            Queue queue = requests.get(id);
            if (queue.containsKey(cmd))
                return;
            queue.put(cmd, new Date().getTime());
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
    }

    static void remove(Context context, String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return;
            Queue queue = requests.get(id);
            if (!queue.containsKey(cmd))
                return;
            queue.remove(cmd);
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
    }

    static void check(Context context, String id) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return;
            boolean found = false;
            Queue queue = requests.get(id);
            CarState state = CarState.get(context, id);
            Set<Map.Entry<CarConfig.Command, Long>> entries = queue.entrySet();
            for (Map.Entry<CarConfig.Command, Long> entry : entries) {
                boolean ok = false;
                CarConfig.Command command = entry.getKey();
                if (command.done != null)
                    ok = State.checkCondition(command.done, state);
                if (!ok && (command.condition != null))
                    ok = !State.checkCondition(command.condition, state);
                if (!ok)
                    continue;
                queue.remove(command);
                found = true;
            }
            if (!found)
                return;
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
    }

    static boolean processSms(Context context, String id, String body) {
        boolean upd = false;
        synchronized (requests) {
            if (!requests.containsKey(id))
                return false;
            boolean found = false;
            Queue queue = requests.get(id);
            CarState state = CarState.get(context, id);
            Set<Map.Entry<CarConfig.Command, Long>> entries = queue.entrySet();
            for (Map.Entry<CarConfig.Command, Long> entry : entries) {
                CarConfig.Command command = entry.getKey();
                if (command.sms == null)
                    continue;
                String[] parts = command.sms.split("\\|");
                if (parts.length < 2)
                    continue;
                Pattern pattern = null;
                try {
                    pattern = Pattern.compile(parts[1]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
                if (pattern == null)
                    continue;
                Matcher matcher = pattern.matcher(body);
                if (!matcher.find())
                    continue;
                found = true;
                if (command.done != null) {
                    Set<String> update = State.update(command.done, state, null);
                    if (update != null) {
                        upd = true;
                        Notification.update(context, id, update);
                    }
                }
                queue.remove(command);
            }
            if (!found)
                return false;
        }
        if (upd) {
            Intent i = new Intent(Names.UPDATED);
            i.putExtra(Names.ID, id);
            context.sendBroadcast(i);
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
        return true;
    }

    static class Queue extends HashMap<CarConfig.Command, Long> {

    }
}
