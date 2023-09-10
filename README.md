# Deep Compare
Deep Compare is a simple Java application for comparing two directory trees and
identifying the differences between them. It can be executed as either a graphical,
point-and-click application with a simple interface, or as a command-line
application for scripted use or "headless" environments.

Deep Compare looks at two attributes when comparing directories:

- The name of each file or subdirectory;
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
oldest (and most compatible) long term support (LTS) release. However, it should
also work for any higher version of Java. While we do use a few compile-time
dependencies (Who wants to write all those getters and setters?), the compiled
JAR is fully self-contained with no dependencies of its own.

## Compiling Deep Compare
Our project uses Maven for its build system. Simply pull down the code and put
Maven to work. Make sure to use the `package` option to generate the JAR file.

## Running Deep Compare
Deep Compare works in one of two modes:

### Graphical Mode (GUI)
If executed on a system with a graphical user interface (GUI) with no command-line
parameters specified, the program goes immediately into GUI mode.  If Java is
configured on your system to automatically execute JAR files (for example, on
Windows, simply double-clicking the JAR), this will also launch the app in GUI
mode.

The initial window is the "Start" window, where the user can specify the main,
required parameters. Optional parameters can be specified by clicking the
**Comparison Options** button, which opens the **Comparison Options** dialog.
Once all parameters are entered, click the **Start** button to begin the
comparison. A progress dialog will appear to keep the user updated on the process.
If desired, the process can be cancelled by clicking the **Cancel** button.

Once the comparison is complete, the results will be displayed. If the two
directories match, a simple message dialog will appear stating as such. If
any discrepancies are found, a more complex **Results Dialog** will appear.
This will have two panels, one for the "source" folder and one for the "target"
folder. The files will be sorted into three categories:

- Files present in one folder but missing from the other;
- Files present in both folders but whose contents differ;
- Files present in both folders and whose contents match.

If you'd like a more permanent record of the comparison, you can use the
comparisons options to specify a location to write a log file to.

### Command-Line Mode (CLI)
If any command-line parameters are provided, or if the program is executed in a
"headless" (non-GUI) environment, Deep Compare automatically goes into
command-line mode (CLI).  Inputs are specified by command-line parameters. In
this mode, three parameters are required:

- The path to the "source" folder for the comparison;
- The path to the "target" folder for the comparison;
- The path to a folder to write the output log file to.

All other parameters are optional.

## Contributing to and Supporting Deep Compare
Deep Compare is intentionally simple and already implements all the features
I was planning to add. However, feature suggestions are welcome.  That said,
I'm not looking to turn this into a Swiss Army Knife of file comparison
tools.

One place where contributions are *definitely* welcome is translations. I
tried hard to make internationalization simple and easy, with virtually all
visible text abstracted into properties files. These files are well commented,
so it will (hopefully) be simple to translate. There's even a dedicated field
for translator attributions, which will be displayed in both the GUI and CLI
modes.

If you like Deep Compare, a simple "thank you" is all I ask in return.
However, if you feel like something more substantial is order, you can send
me a tip using the
[Tip Jar page on my main website](https://www.gpf-comics.com/tips.php).
If you *really* want to make my day, please consider a
[subscription to my online comic strip](https://www.gpf-comics.com/premium/),
which will net you a bunch of fun bonuses as well.