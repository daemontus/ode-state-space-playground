package com.github.sybila.ode.generator

import mpi.Comm
import mpi.Datatype
import mpi.MPI

enum class Type {
    INT, DOUBLE;

    fun getMPJType(): Datatype {
        return when (this) {
            INT -> MPI.INT
            DOUBLE -> MPI.DOUBLE
        }
    }
}
/**
 * We need this primarily for testing.
 */
interface AbstractComm {

    /**
     * Blocking receive message
     */
    fun receive(buffer: Any, offset: Int, size: Int, dataType: Type, source: Int, tag: Int)

    /**
     * Non blocking send
     */
    fun send(buffer: Any, offset: Int, size: Int, dataType: Type, destination: Int, tag: Int)

}

class MPJComm(val comm: Comm) : AbstractComm {

    override fun receive(buffer: Any, offset: Int, size: Int, dataType: Type, source: Int, tag: Int) {
        comm.Recv(buffer, offset, size, dataType.getMPJType(), source, tag)
    }

    override fun send(buffer: Any, offset: Int, size: Int, dataType: Type, destination: Int, tag: Int) {
        comm.Send(buffer, offset, size, dataType.getMPJType(), destination, tag)
    }

}