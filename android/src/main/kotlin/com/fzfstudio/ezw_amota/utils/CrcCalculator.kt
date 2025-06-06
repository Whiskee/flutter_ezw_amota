package com.fzfstudio.ezw_amota.utils

/**
 *  CRC 计算
 */
object CrcCalculator {
    val crc32Table: IntArray? = intArrayOf(
        0x00000000, 0x77073096, -0x11f19ed4, -0x66f6ae46,
        0x076DC419, 0x706AF48F, -0x169c5acb, -0x619b6a5d,
        0x0EDB8832, 0x79DCB8A4, -0x1f2a16e2, -0x682d2678,
        0x09B64C2B, 0x7EB17CBD, -0x1847d2f9, -0x6f40e26f,
        0x1DB71064, 0x6AB020F2, -0xc468eb8, -0x7b41be22,
        0x1ADAD47D, 0x6DDDE4EB, -0xb2b4aaf, -0x7c2c7a39,
        0x136C9856, 0x646BA8C0, -0x29d0686, -0x759a3614,
        0x14015C4F, 0x63066CD9, -0x5f0c29d, -0x72f7f20b,
        0x3B6E20C8, 0x4C69105E, -0x2a9fbe1c, -0x5d988e8e,
        0x3C03E4D1, 0x4B04D447, -0x2df27a03, -0x5af54a95,
        0x35B5A8FA, 0x42B2986C, -0x2444362a, -0x534306c0,
        0x32D86CE3, 0x45DF5C75, -0x2329f231, -0x542ec2a7,
        0x26D930AC, 0x51DE003A, -0x3728ae80, -0x402f9eea,
        0x21B4F4B5, 0x56B3C423, -0x30456a67, -0x47425af1,
        0x2802B89E, 0x5F058808, -0x39f3264e, -0x4ef416dc,
        0x2F6F7C87, 0x58684C11, -0x3e9ee255, -0x4999d2c3,
        0x76DC4190, 0x01DB7106, -0x672ddf44, -0x102aefd6,
        0x71B18589, 0x06B6B51F, -0x60401b5b, -0x17472bcd,
        0x7807C9A2, 0x0F00F934, -0x69f65772, -0x1ef167e8,
        0x7F6A0DBB, 0x086D3D2D, -0x6e9b9369, -0x199ca3ff,
        0x6B6B51F4, 0x1C6C6162, -0x7a9acf28, -0xd9dffb2,
        0x6C0695ED, 0x1B01A57B, -0x7df70b3f, -0xaf03ba9,
        0x65B0D9C6, 0x12B7E950, -0x74414716, -0x3467784,
        0x62DD1DDF, 0x15DA2D49, -0x732c830d, -0x42bb39b,
        0x4DB26158, 0x3AB551CE, -0x5c43ff8c, -0x2b44cf1e,
        0x4ADFA541, 0x3DD895D7, -0x5b2e3b93, -0x2c290b05,
        0x4369E96A, 0x346ED9FC, -0x529877ba, -0x259f4730,
        0x44042D73, 0x33031DE5, -0x55f5b3a1, -0x22f28337,
        0x5005713C, 0x270241AA, -0x41f4eff0, -0x36f3df7a,
        0x5768B525, 0x206F85B3, -0x46992bf7, -0x319e1b61,
        0x5EDEF90E, 0x29D9C998, -0x4f2f67de, -0x3828574c,
        0x59B33D17, 0x2EB40D81, -0x4842a3c5, -0x3f459353,
        -0x12477ce0, -0x65404c4a, 0x03B6E20C, 0x74B1D29A,
        -0x152ab8c7, -0x622d8851, 0x04DB2615, 0x73DC1683,
        -0x1c9cf4ee, -0x6b9bc47c, 0x0D6D6A3E, 0x7A6A5AA8,
        -0x1bf130f5, -0x6cf60063, 0x0A00AE27, 0x7D079EB1,
        -0xff06cbc, -0x78f75c2e, 0x1E01F268, 0x6906C2FE,
        -0x89da8a3, -0x7f9a9835, 0x196C3671, 0x6E6B06E7,
        -0x12be48a, -0x762cd420, 0x10DA7A5A, 0x67DD4ACC,
        -0x6462091, -0x71411007, 0x17B7BE43, 0x60B08ED5,
        -0x29295c18, -0x5e2e6c82, 0x38D8C2C4, 0x4FDFF252,
        -0x2e44980f, -0x5943a899, 0x3FB506DD, 0x48B2364B,
        -0x27f2d426, -0x50f5e4b4, 0x36034AF6, 0x41047A60,
        -0x209f103d, -0x579820ab, 0x316E8EEF, 0x4669BE79,
        -0x349e4c74, -0x43997ce6, 0x256FD2A0, 0x5268E236,
        -0x33f3886b, -0x44f4b8fd, 0x220216B9, 0x5505262F,
        -0x3a45c442, -0x4d42f4d8, 0x2BB45A92, 0x5CB36A04,
        -0x3d280059, -0x4a2f30cf, 0x2CD99E8B, 0x5BDEAE1D,
        -0x649b3d50, -0x139c0dda, 0x756AA39C, 0x026D930A,
        -0x63f6f957, -0x14f1c9c1, 0x72076785, 0x05005713,
        -0x6a40b57e, -0x1d4785ec, 0x7BB12BAE, 0x0CB61B38,
        -0x6d2d7165, -0x1a2a41f3, 0x7CDCEFB7, 0x0BDBDF21,
        -0x792c2d2c, -0xe2b1dbe, 0x68DDB3F8, 0x1FDA836E,
        -0x7e41e933, -0x946d9a5, 0x6FB077E1, 0x18B74777,
        -0x77f7a51a, -0xf09590, 0x66063BCA, 0x11010B5C,
        -0x709a6101, -0x79d5197, 0x616BFFD3, 0x166CCF45,
        -0x5ff51d88, -0x28f22d12, 0x4E048354, 0x3903B3C2,
        -0x5898d99f, -0x2f9fe909, 0x4969474D, 0x3E6E77DB,
        -0x512e95b6, -0x2629a524, 0x40DF0B66, 0x37D83BF0,
        -0x564351ad, -0x2144613b, 0x47B2CF7F, 0x30B5FFE9,
        -0x42420de4, -0x35453d76, 0x53B39330, 0x24B4A3A6,
        -0x452fc9fb, -0x3228f96d, 0x54DE5729, 0x23D967BF,
        -0x4c9985d2, -0x3b9eb548, 0x5D681B02, 0x2A6F2B94,
        -0x4bf441c9, -0x3cf3715f, 0x5A05DF1B, 0x2D02EF8D
    )

    fun calcCrc32(len: Int, buf: ByteArray): Int {
        var crc = -0x1
        var a: Short
        var b: Short
        for (i in 0..<len) {
            a = (buf[i].toInt() and 0xff).toShort()
            b = (crc and 0xff).toShort()
            crc = crc32Table!![a.toInt() xor b.toInt()] xor (crc ushr 8)
        }
        crc = crc xor -0x1
        return crc
    }
}
