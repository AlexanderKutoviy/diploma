package attendance.daemon.dataset;

import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Dataset {

    List<String> getIds();

    /**
     * Returns map id=>row
     */
    Map<String, JSONObject> getRowsByIds(Collection<String> ids);
}
