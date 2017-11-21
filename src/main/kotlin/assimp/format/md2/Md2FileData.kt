/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp.format.md2

import assimp.AI_MAKE_MAGIC
import assimp.AiVector3D
import assimp.logger
import glm_.BYTES
import glm_.c
import java.nio.ByteBuffer

object MD2 {

    // to make it easier for us, we test the magic word against both "endianesses"
    val MAGIC_NUMBER_BE = AI_MAKE_MAGIC("IDP2")
    val MAGIC_NUMBER_LE = AI_MAKE_MAGIC("2PDI")

    // common limitations
    val VERSION = 15
    val MAXQPATH = 64
    val MAX_FRAMES = 512
    val MAX_SKINS = 32
    val MAX_VERTS = 2048
    val MAX_TRIANGLES = 4096

    /** Data structure for the MD2 main header */
    class Header(buffer: ByteBuffer) {
        val magic = buffer.int
        val version = buffer.int
        val skinWidth = buffer.int
        val skinHeight = buffer.int
        val frameSize = buffer.int
        val numSkins = buffer.int
        val numVertices = buffer.int
        val numTexCoords = buffer.int
        val numTriangles = buffer.int
        val numGlCommands = buffer.int
        val numFrames = buffer.int
        val offsetSkins = buffer.int
        val offsetTexCoords = buffer.int
        val offsetTriangles = buffer.int
        val offsetFrames = buffer.int
        val offsetGlCommands = buffer.int
        val offsetEnd = buffer.int

        companion object {
            val size = Int.BYTES * 17
        }
    }

    /** Data structure for a MD2 OpenGl draw command */
    class GLCommand {
        var s = 0f
        var t = 0f
        var vertexIndex = 0
    }

    /** Data structure for a MD2 triangle     */
    class Triangle(buffer: ByteBuffer) {
        val vertexIndices = IntArray(3, { buffer.int })
        val textureIndices = IntArray(3, { buffer.int })

        companion object {
            val size = 6 * Int.BYTES
        }
    }

    /** Data structure for a MD2 vertex     */
    class Vertex(buffer: ByteBuffer) {
        val vertex = IntArray(3, { buffer.int })
        var lightNormalIndex = buffer.int

        companion object {
            val size = 4 * Int.BYTES
        }
    }

    /** Data structure for a MD2 frame */
    class Frame(buffer: ByteBuffer, numVert: Int) {
        val scale = FloatArray(3, { buffer.float })
        val translate = FloatArray(3, { buffer.float })
        var name = String(CharArray(16, { buffer.get().c })).trim()
        val vertices = Array(numVert, { Vertex(buffer) })

        companion object {
            val verticesOffset = 6 * Float.BYTES + 16
            fun size(numVert: Int) = verticesOffset + numVert * Vertex.size
        }
    }

    /** Data structure for a MD2 texture coordinate */
    class TexCoord(buffer: ByteBuffer) {
        var s = buffer.int
        var t = buffer.int

        companion object {
            val size = 2 * Int.BYTES
        }
    }

    /** Data structure for a MD2 skin     */
    class Skin(buffer: ByteBuffer) {
        /* texture file name */
        var name = String(CharArray(MAXQPATH, { buffer.get().c })).trim()

        companion object {
            val size = MD2.MAXQPATH
        }
    }

    /** Lookup a normal vector from Quake's normal lookup table
     *  @param index Input index (0-161)
     *  @return Receives the output normal  */
    fun lookupNormalIndex(index: Int): AiVector3D {
        var i = index * 3
        if (i >= normals.size / 3) // make sure the normal index has a valid value
            i = normals.size / 3 - 1
        // flip z and y to become right-handed
        return AiVector3D(normals, i).apply { val t = z; z = y; y = t; }
    }

    val normals = floatArrayOf(
            -0.525731f, +0.000000f, +0.850651f,
            -0.442863f, +0.238856f, +0.864188f,
            -0.295242f, +0.000000f, +0.955423f,
            -0.309017f, +0.500000f, +0.809017f,
            -0.162460f, +0.262866f, +0.951056f,
            +0.000000f, +0.000000f, +1.000000f,
            +0.000000f, +0.850651f, +0.525731f,
            -0.147621f, +0.716567f, +0.681718f,
            +0.147621f, +0.716567f, +0.681718f,
            +0.000000f, +0.525731f, +0.850651f,
            +0.309017f, +0.500000f, +0.809017f,
            +0.525731f, +0.000000f, +0.850651f,
            +0.295242f, +0.000000f, +0.955423f,
            +0.442863f, +0.238856f, +0.864188f,
            +0.162460f, +0.262866f, +0.951056f,
            -0.681718f, +0.147621f, +0.716567f,
            -0.809017f, +0.309017f, +0.500000f,
            -0.587785f, +0.425325f, +0.688191f,
            -0.850651f, +0.525731f, +0.000000f,
            -0.864188f, +0.442863f, +0.238856f,
            -0.716567f, +0.681718f, +0.147621f,
            -0.688191f, +0.587785f, +0.425325f,
            -0.500000f, +0.809017f, +0.309017f,
            -0.238856f, +0.864188f, +0.442863f,
            -0.425325f, +0.688191f, +0.587785f,
            -0.716567f, +0.681718f, -0.147621f,
            -0.500000f, +0.809017f, -0.309017f,
            -0.525731f, +0.850651f, +0.000000f,
            +0.000000f, +0.850651f, -0.525731f,
            -0.238856f, +0.864188f, -0.442863f,
            +0.000000f, +0.955423f, -0.295242f,
            -0.262866f, +0.951056f, -0.162460f,
            +0.000000f, +1.000000f, +0.000000f,
            +0.000000f, +0.955423f, +0.295242f,
            -0.262866f, +0.951056f, +0.162460f,
            +0.238856f, +0.864188f, +0.442863f,
            +0.262866f, +0.951056f, +0.162460f,
            +0.500000f, +0.809017f, +0.309017f,
            +0.238856f, +0.864188f, -0.442863f,
            +0.262866f, +0.951056f, -0.162460f,
            +0.500000f, +0.809017f, -0.309017f,
            +0.850651f, +0.525731f, +0.000000f,
            +0.716567f, +0.681718f, +0.147621f,
            +0.716567f, +0.681718f, -0.147621f,
            +0.525731f, +0.850651f, +0.000000f,
            +0.425325f, +0.688191f, +0.587785f,
            +0.864188f, +0.442863f, +0.238856f,
            +0.688191f, +0.587785f, +0.425325f,
            +0.809017f, +0.309017f, +0.500000f,
            +0.681718f, +0.147621f, +0.716567f,
            +0.587785f, +0.425325f, +0.688191f,
            +0.955423f, +0.295242f, +0.000000f,
            +1.000000f, +0.000000f, +0.000000f,
            +0.951056f, +0.162460f, +0.262866f,
            +0.850651f, -0.525731f, +0.000000f,
            +0.955423f, -0.295242f, +0.000000f,
            +0.864188f, -0.442863f, +0.238856f,
            +0.951056f, -0.162460f, +0.262866f,
            +0.809017f, -0.309017f, +0.500000f,
            +0.681718f, -0.147621f, +0.716567f,
            +0.850651f, +0.000000f, +0.525731f,
            +0.864188f, +0.442863f, -0.238856f,
            +0.809017f, +0.309017f, -0.500000f,
            +0.951056f, +0.162460f, -0.262866f,
            +0.525731f, +0.000000f, -0.850651f,
            +0.681718f, +0.147621f, -0.716567f,
            +0.681718f, -0.147621f, -0.716567f,
            +0.850651f, +0.000000f, -0.525731f,
            +0.809017f, -0.309017f, -0.500000f,
            +0.864188f, -0.442863f, -0.238856f,
            +0.951056f, -0.162460f, -0.262866f,
            +0.147621f, +0.716567f, -0.681718f,
            +0.309017f, +0.500000f, -0.809017f,
            +0.425325f, +0.688191f, -0.587785f,
            +0.442863f, +0.238856f, -0.864188f,
            +0.587785f, +0.425325f, -0.688191f,
            +0.688191f, +0.587785f, -0.425325f,
            -0.147621f, +0.716567f, -0.681718f,
            -0.309017f, +0.500000f, -0.809017f,
            +0.000000f, +0.525731f, -0.850651f,
            -0.525731f, +0.000000f, -0.850651f,
            -0.442863f, +0.238856f, -0.864188f,
            -0.295242f, +0.000000f, -0.955423f,
            -0.162460f, +0.262866f, -0.951056f,
            +0.000000f, +0.000000f, -1.000000f,
            +0.295242f, +0.000000f, -0.955423f,
            +0.162460f, +0.262866f, -0.951056f,
            -0.442863f, -0.238856f, -0.864188f,
            -0.309017f, -0.500000f, -0.809017f,
            -0.162460f, -0.262866f, -0.951056f,
            +0.000000f, -0.850651f, -0.525731f,
            -0.147621f, -0.716567f, -0.681718f,
            +0.147621f, -0.716567f, -0.681718f,
            +0.000000f, -0.525731f, -0.850651f,
            +0.309017f, -0.500000f, -0.809017f,
            +0.442863f, -0.238856f, -0.864188f,
            +0.162460f, -0.262866f, -0.951056f,
            +0.238856f, -0.864188f, -0.442863f,
            +0.500000f, -0.809017f, -0.309017f,
            +0.425325f, -0.688191f, -0.587785f,
            +0.716567f, -0.681718f, -0.147621f,
            +0.688191f, -0.587785f, -0.425325f,
            +0.587785f, -0.425325f, -0.688191f,
            +0.000000f, -0.955423f, -0.295242f,
            +0.000000f, -1.000000f, +0.000000f,
            +0.262866f, -0.951056f, -0.162460f,
            +0.000000f, -0.850651f, +0.525731f,
            +0.000000f, -0.955423f, +0.295242f,
            +0.238856f, -0.864188f, +0.442863f,
            +0.262866f, -0.951056f, +0.162460f,
            +0.500000f, -0.809017f, +0.309017f,
            +0.716567f, -0.681718f, +0.147621f,
            +0.525731f, -0.850651f, +0.000000f,
            -0.238856f, -0.864188f, -0.442863f,
            -0.500000f, -0.809017f, -0.309017f,
            -0.262866f, -0.951056f, -0.162460f,
            -0.850651f, -0.525731f, +0.000000f,
            -0.716567f, -0.681718f, -0.147621f,
            -0.716567f, -0.681718f, +0.147621f,
            -0.525731f, -0.850651f, +0.000000f,
            -0.500000f, -0.809017f, +0.309017f,
            -0.238856f, -0.864188f, +0.442863f,
            -0.262866f, -0.951056f, +0.162460f,
            -0.864188f, -0.442863f, +0.238856f,
            -0.809017f, -0.309017f, +0.500000f,
            -0.688191f, -0.587785f, +0.425325f,
            -0.681718f, -0.147621f, +0.716567f,
            -0.442863f, -0.238856f, +0.864188f,
            -0.587785f, -0.425325f, +0.688191f,
            -0.309017f, -0.500000f, +0.809017f,
            -0.147621f, -0.716567f, +0.681718f,
            -0.425325f, -0.688191f, +0.587785f,
            -0.162460f, -0.262866f, +0.951056f,
            +0.442863f, -0.238856f, +0.864188f,
            +0.162460f, -0.262866f, +0.951056f,
            +0.309017f, -0.500000f, +0.809017f,
            +0.147621f, -0.716567f, +0.681718f,
            +0.000000f, -0.525731f, +0.850651f,
            +0.425325f, -0.688191f, +0.587785f,
            +0.587785f, -0.425325f, +0.688191f,
            +0.688191f, -0.587785f, +0.425325f,
            -0.955423f, +0.295242f, +0.000000f,
            -0.951056f, +0.162460f, +0.262866f,
            -1.000000f, +0.000000f, +0.000000f,
            -0.850651f, +0.000000f, +0.525731f,
            -0.955423f, -0.295242f, +0.000000f,
            -0.951056f, -0.162460f, +0.262866f,
            -0.864188f, +0.442863f, -0.238856f,
            -0.951056f, +0.162460f, -0.262866f,
            -0.809017f, +0.309017f, -0.500000f,
            -0.864188f, -0.442863f, -0.238856f,
            -0.951056f, -0.162460f, -0.262866f,
            -0.809017f, -0.309017f, -0.500000f,
            -0.681718f, +0.147621f, -0.716567f,
            -0.681718f, -0.147621f, -0.716567f,
            -0.850651f, +0.000000f, -0.525731f,
            -0.688191f, +0.587785f, -0.425325f,
            -0.587785f, +0.425325f, -0.688191f,
            -0.425325f, +0.688191f, -0.587785f,
            -0.425325f, -0.688191f, -0.587785f,
            -0.587785f, -0.425325f, -0.688191f,
            -0.688191f, -0.587785f, -0.425325f)
}