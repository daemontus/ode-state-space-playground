package cz.muni.fi.ode.generator

import cz.muni.fi.checker.Colors
import java.util.Arrays
import java.util.HashSet

//We need our own interval, because we do not allow single points (i.e. (3,3)) as valid intervals
//rest is copied from original Kotlin range
data class Interval(val start: Double, val end: Double) {

    fun isEmpty(): Boolean = start >= end

    infix fun encloses(other: Interval): Boolean =
            (other == this /*Handles empty ranges*/) || (other.start >= this.start && other.end <= this.end)

    infix fun clojure(other: Interval): Interval = when {
        this.isEmpty() -> other
        other.isEmpty() -> this
        else -> Interval(Math.min(this.start, other.start), Math.max(this.end, other.end))
    }

    infix fun intersect(other: Interval): Interval = when {
        this.isEmpty() || other.isEmpty() -> EMPTY
        else -> Interval(Math.max(this.start, other.start), Math.min(this.end, other.end))
    }

    override fun equals(other: Any?): Boolean =
            other is Interval && (isEmpty() && other.isEmpty() ||
                    java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        val result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    companion object {
        /** An empty range of values of type Double. */
        public val EMPTY: Interval = Interval(0.0, 0.0)
    }
}

data class Rect(val ranges: Array<Interval>) {

    companion object {
        fun empty(dim: Int): Rect = Rect(Array(dim, { Interval.EMPTY }))
    }

    infix fun intersect(other: Rect): Rect =
            Rect(Array(ranges.size) { i ->
                this[i] intersect other[i]
            })

    fun isEmpty(): Boolean = ranges.any { it.isEmpty() }

    infix fun encloses(other: Rect): Boolean {
        for (i in ranges.indices) {
            if (!(this[i] encloses other[i])) return false
        }
        return true
    }

    infix fun merge(other: Rect): Rect? {
        if (this.isEmpty()) return other
        if (other.isEmpty()) return this
        var mergeOn = -1
        for (i in ranges.indices) {
            if (this[i] != other[i]) {
                if (mergeOn == -1 && !(this[i] intersect other[i]).isEmpty()) {
                    mergeOn = i
                } else {
                    return null
                    //more than one range is different, or merge dimension
                    //does not share an intersection -> we cannot merge this
                }
            }
        }
        if (mergeOn == -1) return this  //rectangles are equal
        return Rect(Array(ranges.size) { i ->
            if (i == mergeOn) this[i] clojure other[i]
            else this[i]
        })
    }

    infix fun subtract(other: Rect): Set<Rect> {
        if (this.isEmpty() || other.isEmpty()) return setOf(this)
        if (this.intersect(other).isEmpty()) return setOf(this)
        val working = Arrays.copyOf(ranges, ranges.size)
        val result = HashSet<Rect>()
        for (dim in ranges.indices) {
            fun add(it: Interval) {
                if (!it.isEmpty()) {
                    result.add(Rect(Array(ranges.size) { i ->
                        if (i == dim) it
                        else working[i]
                    }))
                }
            }
            add(Interval(this[dim].start, other[dim].start))
            add(Interval(other[dim].end, this[dim].end))
            working[dim] = this[dim] intersect other[dim]
        }
        return result
    }

    operator fun get(i: Int): Interval = ranges[i]
}
/*
data class RectParamSpace(var items: Set<Rect> = setOf()): Colors<RectParamSpace> {

    companion object {
        fun empty() = RectParamSpace()
    }

    override fun intersect(other: RectParamSpace): RectParamSpace {
        val newItems = HashSet<Rect>()
        for (r in items) {
            for (other in other.items) {
                //  println("R: $r O: $other")
                val intersection = r intersect other
                if (!intersection.isEmpty()) newItems.add(intersection)
            }
        }
        this.items = newItems
        normalize()
    }

    override fun minus(other: RectParamSpace): RectParamSpace {
        for (other in other.items) {  //have to update items every iteration so that we perform on space partitioned in previous iteration
            items = items.flatMap { it subtract other }.toSet()
        }
        normalize()
    }

    override fun plus(other: RectParamSpace): RectParamSpace {
        val new = items.union(other.items)
        val old = items
        this.items = new
        normalize()
    }


    fun encloses(set: RectParamSpace): Boolean {
        val o = this.copy()
        o intersect set
        return o == this
    }

    private fun normalize() {

        //remove redundant items
        val notRedundant = HashSet<Rect>()

        for (r in items) {
            var redundant = items.any { it != r && it encloses r }
            if (!redundant) notRedundant.add(r)
        }

        items = notRedundant

        //try to merge items
        val merged = HashSet<Rect>()
        val removed = HashSet<Rect>()

        for (r in items) {

            var merge = items.firstOrNull { it != r && it !in removed && r merge it != null }

            if (merge != null) {
                merged.add(merge.merge(r)!!)
                removed.add(merge)
            } else {
                merged.add(r)
            }
        }

        items = merged

    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }


}*/