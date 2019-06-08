public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable fileTable;
    private int numberOfBlocks;

	public FileSystem(int diskBlocks) {
        // number of total blocks on DISK
        numberOfBlocks = diskBlocks;
	    // create superblock, and format disk with 64 inodes in default
	    superblock = new SuperBlock(diskBlocks);

	    // create directory, and register "/" in directory entry 0
	    directory = new Directory(superblock.totalInodes);

	    // file table is created, and store directory in the file table
	    fileTable = new FileTable(directory);

	    // directory reconstruction
	    /*FileTableEntry dirEnt = open("/", "r");
	    int dirSize = fsize(dirEnt);
	    if (dirSize > 0) {
	    	byte[] dirData = new byte[dirSize];
	    	read(dirEnt, dirData);
	    	directory.bytes2directory(dirData);
	    }
	    close(dirEnt);*/
	}

	public void sync() {
		FileTableEntry localFileTableEntry = open("/", "w");
	    byte[] arrayOfByte = this.directory.directory2bytes();
	    write(localFileTableEntry, arrayOfByte);
	    close(localFileTableEntry);
	    
	    this.superblock.sync();
	}
	
	public boolean format(int files) {
	    
		superblock.format(files);
		
	    createInodes(files);
	    
	    directory = new Directory(numberOfBlocks);
	    
	    fileTable = new FileTable(directory);
	    
	    return true;
	  }

    private boolean createInodes(int files) {
        if (files > 64 || files < 0)
            return false;

        int bNum = files/16;
        short offset = 0;
        for ( int i = 1; i <= bNum; i++) {
            byte[] buf = new byte[Disk.blockSize];
            for (int j = 0; j < 16; j++) {
                Inode toAdd = new Inode();
                SysLib.int2bytes( toAdd.length, buf, offset );
                offset += 4;
                SysLib.short2bytes( toAdd.count, buf, offset );
                offset += 2;
                SysLib.short2bytes( toAdd.flag, buf, offset );
                offset += 2;
                for(int k = 0; k < 11; k++, offset += 2) {
                    SysLib.short2bytes( toAdd.direct[k], buf, offset );
                }
                SysLib.short2bytes( toAdd.indirect, buf, offset);
                offset += 2;
            }
            SysLib.rawwrite(i, buf);
            offset = 0;
        }
        return true;
    }
    
	public FileTableEntry open(String fileName, String mode) {
		FileTableEntry localFileTableEntry = fileTable.falloc(fileName, mode);
	    if ( (mode == "w") && 
	      (!deallocAllBlocks(localFileTableEntry)) ) {
	      return null;
	    }
	    return localFileTableEntry;
	}

    
	public boolean close(FileTableEntry ftEnt) {
		return false;
	}

    
	public int fsize(FileTableEntry ftEnt) {
		return 0;
	}

    
    
	public int read(FileTableEntry ftEnt, byte[] buffer) {
		return 0;
	}

    
	public int write(FileTableEntry ftEnt, byte[] buffer) {
		return 0;
	}

	private boolean deallocAllBlocks(FileTableEntry ftEnt) {
		if (ftEnt.inode.count != 1) {
		      return false;
		    }
		    byte[] arrayOfByte = ftEnt.inode.unregisterIndexBlock();
		    if (arrayOfByte != null) {
		      int i = 0;
		      int j;
		      while ( (j = SysLib.bytes2short(arrayOfByte, i) ) != -1) {
		        this.superblock.returnBlock(j);
		      }
		    }
		    for (int i = 0; i < 11; i++) {
		      if (ftEnt.inode.direct[i] != -1)
		      {
		        this.superblock.returnBlock(ftEnt.inode.direct[i]);
		        ftEnt.inode.direct[i] = -1;
		      }
		    }
		    ftEnt.inode.toDisk(ftEnt.iNumber);
		    return true;
	}

    
	public boolean delete(String fileName) {
		return false;
	}

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

   
	public int seek(FileTableEntry ftEnt, int offset, int whence) {
		return 0;
	}

}
