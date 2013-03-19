#!/usr/bin/env ruby

Dir.chdir(File.join(File.dirname(__FILE__), ".."))

major_version = ""
minor_version = ""

File.open("VERSION") do |fh|
  version = fh.read.strip
  parts = version.split(/[.]/)
  if parts.length == 3
    major_version = parts[0,2].join(".")
    minor_version = parts[2]
  else
    raise Exception.new("Version format changed!")
  end
end

if minor_version == "" || major_version == ""
  raise Exception.new("Could not read version")
else
  major_version.gsub!(/[.]/, "[.]")
end

files = Dir["**/*.md"] + Dir["**/*.txt"]

files.each do |fpath|
  content = ""
  File.open(fpath) do |fin|
    content = fin.read
  end

  updated = content.gsub(/(\D)(#{major_version}[.])(\d+(?:[-]SNAPSHOT)?)/, "\\1\\2#{minor_version}")

  if content != updated
    File.open(fpath, 'w') do |fout|
      fout.write(updated)
      puts "#{fpath} updated"
    end
  end
end
