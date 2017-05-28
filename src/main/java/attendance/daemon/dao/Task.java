package attendance.daemon.dao;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.util.Date;

public class Task {

    public int id;
    public String state;
    public Date timeLastProcessed;
    public JSONObject data;
    public int provider;

    public enum State {
        NEW("new"),
        RUNNING("running"),
        DONE("done");

        public final String name;

        State(String name) {
            this.name = name;
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + state + '\'' +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (id != task.id) return false;
        if (state != null ? !state.equals(task.state) : task.state != null) return false;

        if (data == null) {
            if (task.data != null)
                return false;
        } else {
            if (task.data != null) {
                JSONCompareResult res = JSONCompare.compareJSON(data, task.data, JSONCompareMode.LENIENT);
                if (res.failed())
                    return false;
            }
        }

        return true;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}