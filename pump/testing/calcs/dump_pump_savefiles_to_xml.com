$! read pump_bld.com save files from user$disk:[pettech.pump]*.bld
$! write relevant variables out to stdout in a xml friendly format
$!
$savefile=p1
$set def user$disk:[pettech.pump]
$bld @sys$input
   v
   diam = -99
   rmax = -99
   tbolus = -99
   hl = -99
   tdef = -99
   tmin = -99
   tmax = -99
   kbol = -99
   ds = -99
   prompt_ds = -99
   mass_check = 0
   inject_all_dose = -99
   mw = -99
   wt = -99
   ask_def_act = -99
   desired_def = -99
   desired_warning = -99
   dose_warning = -99
   time_warning = -99
   minvol_warning = -99
   maxvol_warning = -99
   ds = -99
   t = -99
   desired = -99
   dose = -99
   dose0 = -99
   time0 = -99
   vtot = -99
   max_vol_mass = -99
   frac = -99
   vol = -99
   predicted_dose = -99
   dose_predicted = -99
   dose_inj = -99
   rem_vol = -99
   time = 0,0,0
   injtime = 0,0,0
   rec |savefile| 
   %type "<?xml version='1.0' encoding='UTF-8'?>"
   %type "<parms>"
   %type "   <config>"
   %type "      <diam>''diam'</diam>"
   %type "      <rmax>''rmax'</rmax>"
   %type "      <tbolus>''tbolus'</tbolus>"
   %type "      <hl>''hl'</hl>"
   %type "      <tdef>''tdef'</tdef>"
   %type "      <tmin>''tmin'</tmin>"
   %type "      <tmax>''tmax'</tmax>"
   %type "      <kbol>''kbol'</kbol>"
   %type "      <prompt_ds>''prompt_ds'</prompt_ds>"
   %type "      <mass_check>''mass_check'</mass_check>"
   %type "      <inject_all_dose>''inject_all_dose'</inject_all_dose>"
   %type "      <desired_def>''desired_def'</desired_def>"
   %type "      <desired_warning>''desired_warning'</desired_warning>"
   %type "      <dose_warning>''dose_warning'</dose_warning>"
   %type "      <time_warning>''time_warning'</time_warning>"
   %type "      <minvol_warning>''minvol_warning'</minvol_warning>"
   %type "      <maxvol_warning>''maxvol_warning'</maxvol_warning>"
   %type "   </config>"
   %type "   <user>"
   %type "      <ds>''ds'</ds>"
   %type "      <t>''t'</t>"
   %type "      <desired>''desired'</desired>"
   %if ne(dose0;-99) then %type "      <dose0>''dose0'</dose0>"
   %if eq(dose0;-99) then %type "      <dose0>''dose'</dose0>"
   %type "      <time0>''time0(1)':''time0(2)':''time0(3)'</time0>"
   %type "      <vtot>''vtot'</vtot>"
   %type "      <max_vol_mass>''max_vol_mass'</max_vol_mass>"
   %type "      <systime>''time(1)':''time(2)':''time(3)'</systime>"
   %type "      <injtime>''injtime(1)':''injtime(2)':''injtime(3)'</injtime>"
   %type "   </user>"
   %type "   <vax_calc>"
   %type "      <frac>''frac'</frac>"
   %type "      <vol>''vol'</vol>"
   %type "      <dose_inj>''dose_inj'</dose_inj>"
   %type "      <dose_predicted>''dose_predicted'</dose_predicted>"
   %type "      <rem_vol>''rem_vol'</rem_vol>"
   %type "   </vax_calc>"
   %type "   <vax_profile>"
   lines = nrow(times)
   %if gt(lines;2) then %type "      <time3>''times(3)'</time3>"
   %if gt(lines;1) then %type "      <time2>''times(2)'</time2>"
   %if gt(lines;0) then %type "      <time1>''times(1)'</time1>"
   lines = nrow(rates)   
   %if gt(lines;2) then %type "      <rate3>''rates(3)'</rate3>"
   %if gt(lines;1) then %type "      <rate2>''rates(2)'</rate2>"
   %if gt(lines;0) then %type "      <rate1>''rates(1)'</rate1>"
   %type "   </vax_profile>"
   %type "</parms>"
   
   exit
   exit   
$set def user$disk:[pettech.source]
