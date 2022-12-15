#!/NMDSW/bin/perl

sub getUnixDate {
   local $vaxDate = pop(@_);
   
   $vaxDate =~ /([0-9]+)([A-Z]+)([0-9]+)/;
   $vaxDay = $1;
   $vaxMonth = $2;
   $vaxYear = $3;
   
   if (length($vaxDay)==1) { $vaxDay = "0".$vaxDay };
   $convert{"JAN"} = "01";
   $convert{"FEB"} = "02";
   $convert{"MAR"} = "03";
   $convert{"APR"} = "04";
   $convert{"MAY"} = "05";
   $convert{"JUN"} = "06";
   $convert{"JUL"} = "07";
   $convert{"AUG"} = "08";
   $convert{"SEP"} = "09";
   $convert{"OCT"} = "10";
   $convert{"NOV"} = "11";
   $convert{"DEC"} = "12";
   $unixMonth = $convert{$vaxMonth};
   
   $unixDate = "$vaxYear-$unixMonth-$vaxDay";
   return $unixDate;
}



# @info = `cat  zimmerman_1_7may2004_report.dat`;
#    $hours = (split(/[ \n]+/,$info[9]))[2];
#    $mins = (split(/[ \n]+/,$info[10]))[1];
#    $secs = (split(/[ \n]+/,$info[11]))[1];
#    if ( length($hours)==1 ) { $hours = "0".$hours };
#    if ( length($mins )==1 ) { $mins  = "0".$mins  };
#    if ( length($secs )==1 ) { $secs  = "0".$secs  };
#    $assay_time = "$hours:$mins:$secs";
# 
#    print $assay_time ."\n";
# exit 0;



print ",Date,AssayTime,InjTime,ReassayTime,Halflife,Initial Volume,Volume Infused,Assay Dose,Predicted Dose,Actual Dose,% Diff\n";

@list = `ls *.dat`;
#@list = ('adote_1_17aug2004_report.datx','adote_1_20feb2004_report.datx' );
foreach $filename (@list) {
   chop($filename);
   $label = $filename;
   $label =~ s/_report.dat//g;

   @info = `cat $filename`;
   
   if ( $info[4] =~ /^Patient Name/g ) { next; } # exclude dose results on manual infusions
   
   $vaxDate = (split(/:/,$info[4]))[1];
   chop($vaxDate);
   $unixDate=getUnixDate($vaxDate);
   
   $hours = (split(/[ \n]+/,$info[9]))[2];
   $mins = (split(/[ \n]+/,$info[10]))[1];
   $secs = (split(/[ \n]+/,$info[11]))[1];
   if ( length($hours)==1 ) { $hours = "0".$hours };
   if ( length($mins )==1 ) { $mins  = "0".$mins  };
   if ( length($secs )==1 ) { $secs  = "0".$secs  };
   $assayTime = "$hours:$mins:$secs";
   
   $hours = (split(/[ \n]+/,$info[18]))[2];
   $mins = (split(/[ \n]+/,$info[19]))[1];
   $secs = (split(/[ \n]+/,$info[20]))[1];
   if ( length($hours)==1 ) { $hours = "0".$hours };
   if ( length($mins )==1 ) { $mins  = "0".$mins  };
   if ( length($secs )==1 ) { $secs  = "0".$secs  };
   $reassayTime = "$hours:$mins:$secs";
   
   $hours = (split(/[ \n]+/,$info[26]))[3];
   $mins = (split(/[ \n]+/,$info[27]))[1];
   $secs = (split(/[ \n]+/,$info[28]))[1];
   if ( length($hours)==1 ) { $hours = "0".$hours };
   if ( length($mins )==1 ) { $mins  = "0".$mins  };
   if ( length($secs )==1 ) { $secs  = "0".$secs  };
   $injTime = "$hours:$mins:$secs";
   
   $hl = (split(/[ \n]+/,$info[9]))[4];
   $initial_vol = (split(/ +/,$info[9]))[3];
   $infused_vol = (split(/ +/,$info[26]))[5];
   chop($infused_vol);
   $assay_dose = (split(/ +/,$info[9]))[1];
   $predicted_dose = (split(/ +/,$info[26]))[2];
   $actual_dose = (split(/ +/,$info[26]))[4];
   $pctdiff = sprintf("%3.4f",(($actual_dose - $predicted_dose) / $predicted_dose) * 100);

   print "$label,$unixDate,$assayTime,$injTime,$reassayTime,$hl,$initial_vol,$infused_vol,$assay_dose,$predicted_dose,$actual_dose,$pctdiff\n";
}
