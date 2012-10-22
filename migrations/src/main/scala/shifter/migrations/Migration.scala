package shifter.migrations

abstract class Migration(val group: String, val version: Int) {
  def up(): Unit
  def down(): Unit
}


