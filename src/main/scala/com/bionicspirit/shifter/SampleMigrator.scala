package com.bionicspirit.shifter

import java.io._
import com.bionicspirit.utils._


class Version100 extends Migration("Godzilla", 100) {
  def up() {
    println("  - migrated UP to version " + version)
  }

  def down() {
    println("  - migrated DOWN from version " + version)
  }
}

class Version200 extends Migration("Godzilla", 200) {
  def up() {
    println("  - migrated UP to version " + version)
  }

  def down() {
    println("  - migrated DOWN from version " + version)
  }
}

class SampleMigrator extends Migrator("com.bionicspirit.shifter", "Godzilla") {
  private[this] lazy val storage = new File("/tmp/migrations.txt")

  def setup() {
    if (!storage.exists)
      storage.createNewFile
    if (!storage.isFile)
      throw new MigrationException("Storage file is invalid")
    if (!storage.canWrite)
      throw new MigrationException("Storage file is not writable")
    if (!storage.canRead)
      throw new MigrationException("Storage file is not writable")
  }

  def storageReset() {
    setup()
    storage.delete
  }

  def currentVersion: Int = 
    if (storage.exists)
      using (new BufferedReader(new InputStreamReader(new FileInputStream(storage))))(
	in => try { 
	  in.readLine.toInt 
	} catch { 
	  case ex: NumberFormatException => 0 
	})
    else 0
  
  def persistVersion(version: Int) {
    storage.delete
    using (new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storage))))(
      out => out.write(version.toString))
  }
}
