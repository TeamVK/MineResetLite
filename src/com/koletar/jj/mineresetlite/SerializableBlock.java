package com.koletar.jj.mineresetlite;

import com.vk2gpz.mc.material.MaterialUtil;
import com.vk2gpz.mineresetlite.util.MRLUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Optional;

/**
 * @author jjkoletar
 * @author vk2gpz
 */
public class SerializableBlock {
	private final String blockId;
	private final byte data;
	final transient private Material type;
	
	public SerializableBlock(int blockId) {
		this(blockId, (byte) 0);
	}
	
	public SerializableBlock(int blockId, byte data) {
		this(blockId + ":" + data);
	}
	
	public SerializableBlock(String name, byte data) {
		this(name + ":" + data);
	}
	
	public SerializableBlock(String self) {
		String[] bits = self.split(":");
		if (bits.length < 1) {
			throw new IllegalArgumentException("String form of SerializableBlock didn't have sufficient data");
		}
		
		try {
			this.type = MaterialUtil.getMaterial(bits[0]);
			this.data = (bits.length > 1) ? Byte.parseByte(bits[1]) : (byte) 0;
			this.blockId = Optional.ofNullable(this.type).map(Enum::name).orElse(bits[0]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Unable to convert id to integer and data to byte");
		}
	}
	
	public String getBlockId() {
		return blockId;
	}
	
	public byte getData() {
		return data;
	}
	
	public String toString() {
		return blockId + ":" + data;
	}
	
	Material getBlockType() {
		return this.type;
	}
	
	public boolean equals(Object o) {
		return o instanceof SerializableBlock && (this.blockId.equals(((SerializableBlock) o).blockId) && this.data == ((SerializableBlock) o).data);
	}
	
	public void setBlockTypeFor(Block block) {
		MRLUtil.setBlockTypeFor(block, this.blockId, this.type, this.data);
	}
	public static boolean isBlock(String namespeceID, Material type) {
		return MRLUtil.isBlock(namespeceID, type);
	}
}
