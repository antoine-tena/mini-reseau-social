package socialnetwork.back.seirs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SpatialHashGrid {

    private final double cellSize;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Bavard>> cells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> agentCells = new ConcurrentHashMap<>();

    public SpatialHashGrid(double cellSize) {
        this.cellSize = Math.max(8.0, cellSize);
    }

    public double getCellSize() {
        return cellSize;
    }

    public void clear() {
        cells.clear();
        agentCells.clear();
    }

    public void rebuild(Collection<Bavard> agents) {
        clear();
        for (Bavard agent : agents) {
            insert(agent);
        }
    }

    public void insert(Bavard agent) {
        Vector2D position = agent.snapshotPositionRef();
        long key = keyFor(position);
        cells.computeIfAbsent(key, unused -> new CopyOnWriteArrayList<>()).addIfAbsent(agent);
        agentCells.put(agent.getId(), key);
    }

    public void remove(Bavard agent) {
        Long previousKey = agentCells.remove(agent.getId());
        if (previousKey == null) {
            return;
        }
        CopyOnWriteArrayList<Bavard> list = cells.get(previousKey);
        if (list != null) {
            list.remove(agent);
            if (list.isEmpty()) {
                cells.remove(previousKey, list);
            }
        }
    }

    public void updateCell(Bavard agent) {
        long newKey = keyFor(agent.snapshotPositionRef());
        Long oldKey = agentCells.put(agent.getId(), newKey);
        if (oldKey != null && oldKey == newKey) {
            return;
        }
        if (oldKey != null) {
            CopyOnWriteArrayList<Bavard> previousList = cells.get(oldKey);
            if (previousList != null) {
                previousList.remove(agent);
                if (previousList.isEmpty()) {
                    cells.remove(oldKey, previousList);
                }
            }
        }
        cells.computeIfAbsent(newKey, unused -> new CopyOnWriteArrayList<>()).addIfAbsent(agent);
    }

    public List<Bavard> queryRadius(Vector2D position, double radius) {
        return queryRadius(position, radius, null);
    }

    public List<Bavard> queryRadius(Vector2D position, double radius, Bavard self) {
        if (radius <= 0.0) {
            return Collections.emptyList();
        }
        int minX = cellX(position.getX() - radius);
        int maxX = cellX(position.getX() + radius);
        int minY = cellY(position.getY() - radius);
        int maxY = cellY(position.getY() + radius);
        double radiusSquared = radius * radius;

        List<Bavard> result = new ArrayList<>();
        for (int cellX = minX; cellX <= maxX; cellX++) {
            for (int cellY = minY; cellY <= maxY; cellY++) {
                CopyOnWriteArrayList<Bavard> list = cells.get(key(cellX, cellY));
                if (list == null || list.isEmpty()) {
                    continue;
                }
                for (Bavard agent : list) {
                    if (agent == self) {
                        continue;
                    }
                    if (agent.snapshotPositionRef().distanceSquared(position) <= radiusSquared) {
                        result.add(agent);
                    }
                }
            }
        }
        return result;
    }

    public int queryDensity(Vector2D position, double radius) {
        return queryRadius(position, radius).size();
    }

    public int getCellOccupancy(int gridX, int gridY) {
        CopyOnWriteArrayList<Bavard> list = cells.get(key(gridX, gridY));
        return list == null ? 0 : list.size();
    }

    public int getTrackedCellCount() {
        return cells.size();
    }

    private long keyFor(Vector2D position) {
        return key(cellX(position.getX()), cellY(position.getY()));
    }

    private int cellX(double x) {
        return (int) Math.floor(x / cellSize);
    }

    private int cellY(double y) {
        return (int) Math.floor(y / cellSize);
    }

    private long key(int cellX, int cellY) {
        return (((long) cellX) << 32) ^ (cellY & 0xffffffffL);
    }
}
