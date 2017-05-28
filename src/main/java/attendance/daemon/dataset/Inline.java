package attendance.daemon.dataset;

import org.json.JSONObject;
import tit.utils.mapper.NameProperty;

import java.util.*;

public class Inline implements Dataset {

    @NameProperty
    public List<JSONObject> rows;

    @Override
    public List<String> getIds() {
        ArrayList<String> res = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); ++i) {
            JSONObject row = rows.get(i);

            String id = row.optString("id");
            if (id == null)
                id = row.getString("address");

            res.add(id);
        }
        return res;
    }

    @Override
    public Map<String, JSONObject> getRowsByIds(Collection<String> ids) {
        HashMap<String, JSONObject> res = new HashMap<>();
        for (int i = 0; i < rows.size(); ++i) {
            JSONObject row = rows.get(i);

            String id = row.optString("id");
            if (id == null)
                id = row.getString("address");

            if (ids.contains(id))
                res.put(id, row);
        }
        return res;
    }
}
