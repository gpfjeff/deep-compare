# Deep Compare
Deep Compare is a simple Java application for comparing two directory trees and
identifying the differences between them. It can be executed as either a graphical,
point-and-click application with a simple interface, or as a command-line
application for scripted use or "headless" environments.

Deep Compare looks at two attributes when comparing directories:

- The name of the each file or sub-directory;
- The contents of a file or, more accurately, a cryptographic hash of its contents.

Two directories are considered to "match" if there are no "missing" files (i.e.,
files that are in one directory but not the other) ***AND*** all comparable files
in both paths have the same contents (i.e., their cryptographic hashes match).
File timestamps are ***NOT*** considered, since they may not be accurate. (When
you copy a file, some file systems preserve the original file's timestamp on the
copy, while others assign the file a new timestamp as it is "created".)

Yes, there are other tools in existence that do what this little app does. That
said, none of them do exactly what I was looking for and for the right price
(i.e., free), so I decided to write my own.

Deep Compare is written for Java 8, which (at the time of this writing) is the
oldest long term support (LTS) release. However, it should also work for any
higher version of Java.