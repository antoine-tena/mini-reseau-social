package socialnetwork.back.seirs;

import java.util.Objects;
import java.util.Random;
import java.util.SplittableRandom;

public final class Vector2D {

    public static final Vector2D ZERO = new Vector2D(0.0, 0.0);
    public static final Vector2D UNIT_X = new Vector2D(1.0, 0.0);
    public static final Vector2D UNIT_Y = new Vector2D(0.0, 1.0);

    private double x;
    private double y;

    public Vector2D() {
        this(0.0, 0.0);
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        this(other.x, other.y);
    }

    public static Vector2D of(double x, double y) {
        return new Vector2D(x, y);
    }

    public static Vector2D fromAngle(double radians) {
        return new Vector2D(Math.cos(radians), Math.sin(radians));
    }

    public static Vector2D randomUnit(Random random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        return fromAngle(angle);
    }

    public static Vector2D randomUnit(SplittableRandom random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        return fromAngle(angle);
    }

    public static Vector2D randomInDisk(Random random, double radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        return new Vector2D(Math.cos(angle) * dist, Math.sin(angle) * dist);
    }

    public static Vector2D randomInDisk(SplittableRandom random, double radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        return new Vector2D(Math.cos(angle) * dist, Math.sin(angle) * dist);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vector2D set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2D set(Vector2D other) {
        this.x = other.x;
        this.y = other.y;
        return this;
    }

    public Vector2D copy() {
        return new Vector2D(x, y);
    }

    public Vector2D addLocal(Vector2D other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector2D addLocal(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public Vector2D subtractLocal(Vector2D other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    public Vector2D multiplyLocal(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public Vector2D divideLocal(double scalar) {
        if (scalar != 0.0) {
            this.x /= scalar;
            this.y /= scalar;
        }
        return this;
    }

    public Vector2D negateLocal() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    public Vector2D normalizeLocal() {
        double len = length();
        if (len > 1.0e-9) {
            this.x /= len;
            this.y /= len;
        }
        return this;
    }

    public Vector2D withLengthLocal(double length) {
        return normalizeLocal().multiplyLocal(length);
    }

    public Vector2D limitLocal(double max) {
        double lenSq = lengthSquared();
        double maxSq = max * max;
        if (lenSq > maxSq && lenSq > 1.0e-9) {
            double ratio = max / Math.sqrt(lenSq);
            this.x *= ratio;
            this.y *= ratio;
        }
        return this;
    }

    public Vector2D clampLocal(double minLength, double maxLength) {
        double len = length();
        if (len < 1.0e-9) {
            return this;
        }
        if (len < minLength) {
            return multiplyLocal(minLength / len);
        }
        if (len > maxLength) {
            return multiplyLocal(maxLength / len);
        }
        return this;
    }

    public Vector2D rotateLocal(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double nx = x * cos - y * sin;
        double ny = x * sin + y * cos;
        this.x = nx;
        this.y = ny;
        return this;
    }

    public Vector2D floorLocal() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        return this;
    }

    public Vector2D ceilLocal() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    public Vector2D lerpLocal(Vector2D target, double alpha) {
        this.x += (target.x - this.x) * alpha;
        this.y += (target.y - this.y) * alpha;
        return this;
    }

    public Vector2D jitterLocal(SplittableRandom random, double amplitude) {
        this.x += random.nextDouble(-amplitude, amplitude);
        this.y += random.nextDouble(-amplitude, amplitude);
        return this;
    }

    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return new Vector2D(this.x - other.x, this.y - other.y);
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    public Vector2D divide(double scalar) {
        return scalar == 0.0 ? new Vector2D(this) : new Vector2D(this.x / scalar, this.y / scalar);
    }

    public Vector2D negate() {
        return new Vector2D(-x, -y);
    }

    public Vector2D normalized() {
        double len = length();
        return len <= 1.0e-9 ? new Vector2D() : new Vector2D(x / len, y / len);
    }

    public Vector2D withLength(double targetLength) {
        double len = length();
        return len <= 1.0e-9 ? new Vector2D() : multiply(targetLength / len);
    }

    public Vector2D limited(double max) {
        double lenSq = lengthSquared();
        if (lenSq <= max * max) {
            return new Vector2D(this);
        }
        return normalized().multiply(max);
    }

    public Vector2D rotated(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }

    public Vector2D perpendicular() {
        return new Vector2D(-y, x);
    }

    public Vector2D midpoint(Vector2D other) {
        return new Vector2D((x + other.x) * 0.5, (y + other.y) * 0.5);
    }

    public Vector2D lerp(Vector2D target, double alpha) {
        return new Vector2D(x + (target.x - x) * alpha, y + (target.y - y) * alpha);
    }

    public Vector2D projectOnto(Vector2D axis) {
        double denom = axis.lengthSquared();
        if (denom <= 1.0e-9) {
            return new Vector2D();
        }
        double scalar = dot(axis) / denom;
        return axis.multiply(scalar);
    }

    public Vector2D reflect(Vector2D normal) {
        Vector2D n = normal.normalized();
        return subtract(n.multiply(2.0 * dot(n)));
    }

    public double dot(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    public double cross(Vector2D other) {
        return this.x * other.y - this.y * other.x;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double distance(Vector2D other) {
        return Math.sqrt(distanceSquared(other));
    }

    public double distanceSquared(Vector2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    public double manhattanDistance(Vector2D other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public double angle() {
        return Math.atan2(y, x);
    }

    public double angleTo(Vector2D other) {
        return Math.atan2(cross(other), dot(other));
    }

    public boolean isZero(double epsilon) {
        return Math.abs(x) <= epsilon && Math.abs(y) <= epsilon;
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    public Vector2D abs() {
        return new Vector2D(Math.abs(x), Math.abs(y));
    }

    public Vector2D min(Vector2D other) {
        return new Vector2D(Math.min(x, other.x), Math.min(y, other.y));
    }

    public Vector2D max(Vector2D other) {
        return new Vector2D(Math.max(x, other.x), Math.max(y, other.y));
    }

    public Vector2D clampToBounds(double minX, double minY, double maxX, double maxY) {
        return new Vector2D(
            Math.max(minX, Math.min(maxX, x)),
            Math.max(minY, Math.min(maxY, y))
        );
    }

    public Vector2D clampToBoundsLocal(double minX, double minY, double maxX, double maxY) {
        this.x = Math.max(minX, Math.min(maxX, x));
        this.y = Math.max(minY, Math.min(maxY, y));
        return this;
    }

    public boolean epsilonEquals(Vector2D other, double epsilon) {
        return Math.abs(this.x - other.x) <= epsilon && Math.abs(this.y - other.y) <= epsilon;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vector2D vector2D)) {
            return false;
        }
        return Double.compare(vector2D.x, x) == 0 && Double.compare(vector2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Vector2D{" + "x=" + x + ", y=" + y + '}';
    }
}
