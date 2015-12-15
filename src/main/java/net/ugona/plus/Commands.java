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
            Set<Map.Entry<CarConfig.Command, CommandState>> entries = queue.entrySet();
            for (Map.Entry<CarConfig.Command, CommandState> entry : entries) {
                if (entry.getValue().time + 900000 < now) {
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

    static void put(Context context, String id, CarConfig.Command cmd, Intent data) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                requests.put(id, new Queue());
            Queue queue = requests.get(id);
            if (queue.containsKey(cmd))
                return;
            CommandState state = new CommandState();
            state.time = new Date().getTime();
            state.data = data;
            queue.put(cmd, state);
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
    }

    static Intent remove(Context context, String id, CarConfig.Command cmd) {
        Intent data = null;
        synchronized (requests) {
            if (!requests.containsKey(id))
                return null;
            Queue queue = requests.get(id);
            if (!queue.containsKey(cmd))
                return null;
            data = queue.get(cmd).data;
            queue.remove(cmd);
        }
        Intent i = new Intent(Names.COMMANDS);
        i.putExtra(Names.ID, id);
        context.sendBroadcast(i);
        return data;
    }

    static void setId(Context context, String car_id, CarConfig.Command cmd, int cmd_id) {
        synchronized (requests) {
            if (!requests.containsKey(car_id))
                return;
            Queue queue = requests.get(car_id);
            if (!queue.containsKey(cmd))
                return;
            queue.get(cmd).id = cmd_id;
            Intent intent = new Intent(context, FetchService.class);
            intent.setAction(FetchService.ACTION_CHECK_COMMAND);
            intent.putExtra(Names.ID, car_id);
            intent.putExtra(Names.COMMAND, cmd.id);
            context.startService(intent);
        }
    }

    static int getId(Context context, String car_id, int cmd_id) {
        synchronized (requests) {
            if (!requests.containsKey(car_id))
                return 0;
            Queue queue = requests.get(car_id);
            CarConfig config = CarConfig.get(context, car_id);
            CarConfig.Command[] commands = config.getCmd();
            for (CarConfig.Command c : commands) {
                if (c.id == cmd_id) {
                    CommandState state = queue.get(c);
                    if (state == null)
                        return 0;
                    long now = new Date().getTime();
                    if (state.time + 900000 < now)
                        return 0;
                    return queue.get(c).id;
                }
            }
            return 0;
        }
    }

    static void setCommandResult(Context context, String car_id, int cmd, int result, String message) {
        CarConfig carConfig = CarConfig.get(context, car_id);
        CarConfig.Command[] commands = carConfig.getCmd();
        for (CarConfig.Command c : commands) {
            if (c.id == cmd) {
                Commands.remove(context, car_id, c);
                if (result > 0) {
                    String text = message;
                    if (text == null)
                        text = context.getString(R.string.cmd_error);
                    String actions = FetchService.ACTION_COMMAND + ";";
                    actions += cmd + "|" + "|1:" + android.R.drawable.ic_popup_sync + ":" + context.getString(R.string.retry);
                    Notification.create(context, text, R.drawable.w_warning_light, car_id, null, 0, false, c.name, actions);
                } else if (result == 0) {
                    if (c.onAnswer != null) {
                        c.onAnswer.run();
                    } else if (c.done != null) {
                        CarState state = CarState.get(context, car_id);
                        Set<String> update = State.update(context, car_id, c, c.done, state, null, null);
                        if (update != null) {
                            Notification.update(context, car_id, update);
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        }
                    }
                }
                break;
            }
        }
    }

    static void check(Context context, String id) {
        synchronized (requests) {
            if (!requests.containsKey(id))
                return;
            boolean found = false;
            Queue queue = requests.get(id);
            CarState state = CarState.get(context, id);
            Set<Map.Entry<CarConfig.Command, CommandState>> entries = queue.entrySet();
            for (Map.Entry<CarConfig.Command, CommandState> entry : entries) {
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
                if (command.time != null) {
                    Intent intent = new Intent(context, FetchService.class);
                    intent.setAction(FetchService.ACTION_ADD_TIMER);
                    intent.putExtra(Names.ID, id);
                    intent.putExtra(Names.COMMAND, command.time);
                    context.startService(intent);
                }
                break;
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
            Set<Map.Entry<CarConfig.Command, CommandState>> entries = queue.entrySet();
            body = body.toUpperCase();
            for (Map.Entry<CarConfig.Command, CommandState> entry : entries) {
                CarConfig.Command command = entry.getKey();
                if (command.sms == null)
                    continue;
                String[] parts = command.sms.split("\\|");
                int i;
                Matcher matcher = null;
                boolean bError = false;
                for (i = 1; i < parts.length; i++) {
                    String part = parts[i];
                    bError = false;
                    if (part.substring(0, 1).equals("!")) {
                        bError = true;
                        part = part.substring(1);
                    }
                    Pattern pattern = null;
                    try {
                        pattern = Pattern.compile(part);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    if (pattern == null)
                        continue;
                    matcher = pattern.matcher(body);
                    if (matcher.find())
                        break;
                }
                if (i >= parts.length)
                    continue;
                found = true;
                queue.remove(command);
                if (bError) {
                    String text = context.getString(R.string.cmd_error);
                    String actions = FetchService.ACTION_COMMAND + ";";
                    actions += command.id + "|" + "|:" + android.R.drawable.ic_popup_sync + ":" + context.getString(R.string.retry);
                    Notification.create(context, text, R.drawable.w_warning_light, id, null, 0, false, command.name, actions);
                    break;
                }
                if (command.onAnswer != null) {
                    command.onAnswer.run();
                } else if (command.done != null) {
                    Set<String> update = State.update(context, id, command, command.done, state, matcher, entry.getValue());
                    if (update != null) {
                        upd = true;
                        Notification.update(context, id, update);
                    }
                }
                if (command.time != null) {
                    Intent intent = new Intent(context, FetchService.class);
                    intent.setAction(FetchService.ACTION_ADD_TIMER);
                    intent.putExtra(Names.ID, id);
                    intent.putExtra(Names.COMMAND, command.time);
                    context.startService(intent);
                }
                break;
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

    static class CommandState {
        long time;
        int id;
        Intent data;
    }

    static class Queue extends HashMap<CarConfig.Command, CommandState> {

    }
}
