917b34a Delete macro print jobs after transfer to printer.
9b29cd6 Fixes issue ROB-95 - Macros need to be pausable, resumeable and cancellable.
0de96bc Fixes issue ROB-90 - Switch head LED on after heating and off after calibration.
a28976f Fixes issue ROB-91 - Test on nozzle calibration page pushes buttons off bottom
9ef4172 Fixes issue ROB-70 - Reprint is allowed when an amendment has been made to the custom profile
c5a03f6 Fixes issue ROB-92 - 1st text on nozzle height is wrong... nozzle offset calibration
42e4291 Fixes issue ROB-30 - When Custom Profile is selected open the advanced slider
d4a121d Enhanced settings slideout so that the material/profile tabs are selected when a material is selected or the print quality is changed.
7213daf Fixes ROB-70 - Reprint is allowed when an amendment has been made to a custom profile
a200a38 Fixed error in purge task - since the firmware raises an error when a macro command is issued (if a purge is required) - it is necessary to set the new head eeprom values first.
4954e3c Merge origin/master
6f456a5 Fixes issue ROB-7 - Save last file location for all explorer prompts.
a046159 Update URL for software support page
e87fb71 Merge branch 'master' of https://bitbucket.org/celrobox/celtechcore
9cba1fc Merged Sprint2 into master
697a1e1 Commit for synch
afc880f Change printer detection on linux
0b8fbc5 Merge origin/Sprint2
dd69895 Fixes ROB-19 - Delete key should delete selected part from plate.
620703a Handle machine type not recognised Handle 3D support not available
efbaebe Added javadoc for modified code.
5b80d07 Fixes Task ROB-87 Print button should not be available if head is not attached.
92856f4 Merged Sprint1 into master
41aee8f Minor updates to improve debugging + increase size of translation drag plane to improve 3d manipulation.
6be6d51 Allow ' and <space> in printer ID entry box
f5f036a Fixed Task #ROB-78 - Choose printer - saving name doesn't work + colour order
1a8a469 Fixed Task #ROB-66 - Macros or calibration sequences shouldn't be executable during a print job
e539d1c Fixed Task #ROB-66 - Macros or calibration sequences shouldn't be executable during a print job
daa6ff9 Fixed Task #ROB-65 - Macro transfer didn't close dialog box
ed2ffaa Fixed Task #ROB-64 - Hardware Pause during print job transfer doesn't show on screen
cc5d668 Fixed issue where loaded filament doesn't show up in settings slideout
89bc084 Fixes Task#ROB-18 - post-processor incorrectly assigns nozzles when T0 only is selected. NB - this was caused by Slic3r not outputting any T commands.
f967820 Fixes Task #ROB-83 - extruder buttons should use G1 F100. Now configurable as part of JogButton fxml
6fa200d Fixes Task #ROB-14 - print job can be started without filament loaded
1fdad56 Fixes Task #ROB-4 pause then abort and Task #ROB-82 Auto purge
39503dc Projects now load/save from where they were opened (rather than being copied to the Projects directory) Task #ROB-1 - Projects do not load after quit
4bf8d59 
95e6107 Autoupdate changed to use HTTP request - gets rid of authorisation popup...
e0f2713 Integrated why are we waiting and interlocks. LED colour display and printer commands are now mapped as printer does not display correct values.
a0df5ce Fixed post processor nozzle override and output of subsequent layer temperature.
ac8beb1 Remove spiral vase option from print profile page.
f21e9ac First Beta configuration.
980e1b8 
76ea248 Print/pause fixed. Note - hardware pause in any state other than printing is not supported.
816734b Changed background colour of slideouts to dark grey. Autoselect models when they are loaded.
3279458 Mods to text on cancelling slicer and post processor. Macro send now doesn't wait - just blats out data. Macro send now uses new gcode preparation pre-transmission - as does send direct and print. Loaded filament now shows correct remaining length.
fd30dad Shutdown when transferring data now gated. Added a task controller that kills tasks if they haven't finished 5 secs into shutdown.
ee87845 EEPROM data now includes temperature of last filament.
06db4c8 Maintenance page now consolidates macros, gcode and calibration. Firmware updates fixed and memory put in place for remaining file choosers.
43641d0 Final (!) fixes to autoupdate. Updater now runs in attended mode - unattended mode failed - BitRock problem?
2e31c56 Cleaned up old display elements. Added memory to GCode file chooser.
99227a0 Status tab graphic changed to invert colours when status tab is displayed. Enabled AutoUpdate.
584c7f7 Door open confirmation added when bed temperature above 60 degrees. G37 now used and abort print is sent if hot and confirmation is given. Pause and resume now work interchangeably from hardware/software.
4d4e585 Nozzle and Bed temperatures below 35 degrees are now shown as 'Cold' on the printer status side panel.
9c06ac2 
97c78aa B calibration incorporated
c108788 post-processor changed to volumetric close.
15528fe v0.02.02 Configurator v0.05.02 AutoMaker. Pre-processor now operational.
68f3e56 Major update. Included post-processor for GCode - requires vanilla Slic3r. This version not working - mid change - stored for safety.
8cc7489 Major update. v1.57 of Configurator
6e44362 Major update.
1f590bc Final version of printer status page in place.
b935163 Version 0.01.44j
7065637 Version 0.01.44f
dd726c8 Version 0.01.44b
096e956 Initial maven commit
