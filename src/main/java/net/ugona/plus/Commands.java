package net.ugona.plus;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.HashSet;
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
            return queue.contains(cmd);
        }
    }

    static void put(Context context, String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                requests.put(id, new Queue());
            Queue queue = requests.get(id);
            if (queue.contains(id))
                return;
            queue.add(cmd);
        }
        Intent i = new Intent(Names.COMMAND);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
    }

    static void remove(Context context, String id, CarConfig.Command cmd) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return;
            Queue queue = requests.get(id);
            if (!queue.contains(id))
                return;
            queue.remove(cmd);
        }
        Intent i = new Intent(Names.COMMAND);
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
            for (CarConfig.Command c : queue) {
                if (c.done == null)
                    continue;
                if (!State.checkCondition(c.done, state))
                    continue;
                queue.remove(c);
                found = true;
            }
            if (!found)
                return;
        }
        Intent i = new Intent(Names.COMMAND);
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
            for (CarConfig.Command c : queue) {
                if (c.sms == null)
                    continue;
                String[] parts = c.sms.split("\\!");
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
                if (c.done != null) {
                    Set<String> update = State.update(c.done, state);
                    if (update != null)
                        upd = true;
                }
                queue.remove(c);
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

    static class Queue extends HashSet<CarConfig.Command> {

    }
}
