# Deep Compare
Deep Compare is a simple Java application for comparing two directory trees and
identifying the differences between them. It can be executed as either a graphical,
point-and-click application with a simple interface, or as a command-line
application for scripted use or "headless" environments.

For the comparison, one folder is arbitrarily designated as the "source"
while the other is designated as the "target". Deep Compare looks at two attributes
when comparing these directories:

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
dependencies (Who wants to manually write all those getters and setters?), the
compiled JAR is fully self-contained with no dependencies of its own.

## Compiling Deep Compare
Our project uses Maven for its build system. Simply pull down the code and put
Maven to work. Make sure to use the `package` goal to generate the JAR file.

## Installing Deep Compare
There is no installer, and we do not define an `install` Maven goal. It's up
to the user to "install" Deep Compare where they want it.

## Running Deep Compare
Deep Compare works in one of two modes:

### Graphical Mode (GUI)
If executed on a system with a graphical user interface (GUI) with no command-line
parameters specified, the program goes immediately into GUI mode:

`java -jar deep-compare-[version].jar`

Windows users may want to use the `javaw` command rather than `java` if setting
up a desktop shortcut to avoid opening an extra terminal window.

If Java is configured on your system to automatically execute JAR files (for
example, on Windows, simply double-clicking the JAR), this will also launch the
app in GUI mode.

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

All other parameters are optional. Use the `--help` parameter to see a list of
all available parameters.

`java -jar deep-compare-[version].jar --help`

## Comparison Options
As stated above, the only two mandatory pieces of information required are the
source and target directories. Which folder you designate as the "source" versus
the "target" is entirely up to you and may be an arbitrary decision. When running
in CLI mode, an output log file path is also required; this is optional in GUI
mode. Other options are outlined below.

### Exclusions
There may be certain files or file types you want to ignore or exclude from the
comparison. For example, Microsoft Windows has an annoying tendency to add a
"Thumbs.db" file whenever you browse a folder in Windows Explorer when thumbnail
support is enabled. These may exist in your source folder, but not the target,
such as an external backup drive. You may want to ignore these files when
checking to see if the backup copy is up to date.

Exclusions are defined in one of two modes:

- DOS/UNIX "glob" patterns. For example, question marks ("?") represent a single character, where as asterisks ("*") can stand in for any number of characters.
- If simple wildcards won't do, you can use Java compatible regular expressions for more complex filters.

Note that DOX/UNIX style "glob" patterns will be converted to regular expressions
internally before the comparison runs.

The type of exclusion pattern is defined by a drop-down option in the GUI or a
command-line switch in CLI mode. In the GUI, exclusions can be added interactively
via a small panel in the **Comparison Options** dialog. In CLI mode, they must be
specified in an external file, whose path must be specified on the command line.
The exclusion file should have one pattern per line. Empty lines and those
starting with a hash or pound sign ("#") will be ignored. You can add hashes to
your file to denote comments, if you'd like.

Exclusions are processed in the specified order. In the GUI, you can move the
exclusions patterns up and down to rearrange them.  In CLI mode, the order is
specified by the order in the file. A file could match multiple exclusion
patterns, but all patterns after the first match will be ignored.

### Cryptographic Hashing Algorithm
Rather than comparing the raw contents of files, Deep Compare performs a
cryptographic hash of each one and compares the hashes. Since even tiny changes
to a file results in vastly different hash results, this is a secure way of
checking to see if the files differ.

The hash algorithms available depend on the version of Java you have installed.
Some hashes may not be available on all runtimes or even platforms. In CLI mode,
you can check to see which hashes are available by using the `--show-hashes`
option. In GUI mode, the list of available hashes will be in a drop-down list
in the **Comparison Options** dialog.

By default, Deep Compare tries to use SHA-256, but will fall back to
SHA-1 if that is not available. Which hash algorithm you decide to use is
entirely up to you. Note, however, that some algorithms are considered more
secure than others. If security is a concern, SHA2 or SHA3 hashes are strongly
recommended.

### Hidden Files
By default, Deep Compare ignores hidden files. You can force it to consider
hidden files by checking the relevant checkbox in the GUI or using the
command-line parameter in CLI mode.

What files are considered "hidden" depends on you operating system. Consult
your OS documentation for more details.

### Optional Log File
A log file is required in CLI mode, as that is the primary way Deep Compare
will report its results. However, you can turn this on in GUI mode in the
**Comparison Options** dialog.

As in CLI mode, you must specify an output folder to write the log file to.
Deep Compare will create a `deep-compare.log` file in that folder and will
overwrite any file by the same name if one already exists. The log folder
must exist outside of both the source and target directories.

### Debug Mode
Both the GUI and CLI modes have a toggle for adding extra debug information to
the log file. This mostly consists of more detailed error information such as
raw Java exceptions and stack traces. The debug mode toggle is largely ignored
in GUI mode if the optional log file is not specified.

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