/*
 * inga_demo.c
 *
 *  Created on: 13.10.2009
 *      Author: Kulau

 * 
 *
 */


#include "contiki.h"
#include "acc-sensor.h"
#include "gyro-sensor.h"
#include "pressure-sensor.h"
#include "battery-sensor.h"
#include "mag-sensor.h"
#include "dev/sdcard.h"           //tested

#include <util/delay.h>
#include <stdio.h>
#include <stdlib.h>

#include <avr/io.h>
extern uint8_t __bss_end;

#define PWR_MONITOR_ICC_ADC             3
#define PWR_MONITOR_VCC_ADC             2

#define DEBUG                                   1

/*---------------------------------------------------------------------------*/
PROCESS(default_app_process, "Hello world process");
AUTOSTART_PROCESSES(&default_app_process);
/*---------------------------------------------------------------------------*/
static struct etimer timer;
PROCESS_THREAD(default_app_process, ev, data)
{
  PROCESS_BEGIN();

  _delay_ms(100); //just wait a short period to ensure are sensors are available
  
  static const struct sensors_sensor *acc_sensor;
  static const struct sensors_sensor *press_sensor;
  static const struct sensors_sensor *gyro_sensor;
  static const struct sensors_sensor *batt_sensor;
  static const struct sensors_sensor *mag_sensor;  
  

#if DEBUG
  printf("Begin initialization:\n");
  SDCARD_POWER_ON();
  if (sdcard_init() == 0) {
    printf(":microSD   OK\n");
    SDCARD_POWER_OFF();
  } else {
    printf(":microSD   FAILURE\n");
  }
  acc_sensor = sensors_find("Acc");
  uint8_t acc_status = SENSORS_ACTIVATE(*acc_sensor);
  if (acc_status == 0) printf("Error: Failed to init accelerometer\n");
  else printf("Accelerometer: OK\n");
  
  press_sensor = sensors_find("Press");
  uint8_t press_status = SENSORS_ACTIVATE(*press_sensor);
  if (press_status == 0) printf("Error: Failed to init pressure sensor\n"); 
  else printf("Pressure sensor: OK\n");
  
  gyro_sensor = sensors_find("Gyro");
  uint8_t gyro_status = SENSORS_ACTIVATE(*gyro_sensor);
  if (gyro_status == 0) printf("Error: Failed to init gyroscope\n");
  else printf("Gyroscrope: OK\n");
  
  batt_sensor = sensors_find("Batt");
  uint8_t batt_status = SENSORS_ACTIVATE(*batt_sensor);
  if (batt_status == 0) printf("Error: Failed to init battery-sensor\n");
  else printf("Battery sensor: OK\n");

  mag_sensor = sensors_find("Mag");
  uint8_t mag_status = SENSORS_ACTIVATE(*mag_sensor);
  if (mag_status == 0) printf("Error: Failed to init magnetic sensor\n");
  else printf("Magnetic sensor: OK\n");

//  if (bmp085_init() == 0) {
//    printf(":BMP085    OK\n");
//  } else {
//    printf(":BMP085    FAILURE\n");
//  }
//  if (l3g4200d_init() == 0) {
//    printf(":L3G4200D  OK\n");
//  } else {
//    printf(":L3G4200D  FAILURE\n");
//  }
#else
  microSD_switchoff();
  microSD_init();
 
  acc_sensor = sensors_find("Acc");
  uint8_t acc_status = SENSORS_ACTIVATE(*acc_sensor);
  
  press_sensor = sensors_find("Press");
  uint8_t press_status = SENSORS_ACTIVATE(*press_sensor);
  
  gyro_sensor = sensors_find("Gyro");
  uint8_t gyro_status = SENSORS_ACTIVATE(*gyro_sensor);

  batt_sensor = sensors_find("Batt");
  uint8_t batt_status = SENSORS_ACTIVATE(*batt_sensor);

  mag_sensor = sensors_find("Mag");
  uint8_t mag_status = SENSORS_ACTIVATE(*mag_sensor);
#endif

  etimer_set(&timer, 0.05*CLOCK_SECOND);
  
  while (1) {
    PROCESS_YIELD();
    etimer_set(&timer, CLOCK_SECOND);

    /*############################################################*/
    //Accelerometer serial Test
    printf("Accelerometer - X:%+6d | Y:%+6d | Z:%+6d\n",
            acc_sensor->value(ACC_X),
            acc_sensor->value(ACC_Y),
            acc_sensor->value(ACC_Z));

    /*############################################################*/
    //Gyroscop serial Test
    printf("Gyroscope - X:%+6d | Y:%+6d | Z:%+6d\n",
            gyro_sensor->value(GYRO_X),
            gyro_sensor->value(GYRO_Y),
            gyro_sensor->value(GYRO_Z));
    printf("Gyro Temperature:%+3d\n", gyro_sensor->value(GYRO_TEMP));

    /*############################################################*/
    //Pressure serial Test 
    int32_t pressval = ((int32_t) press_sensor->value(PRESS_H) << 16);
    pressval |= (press_sensor->value(PRESS_L) & 0xFFFF);
    printf("Pressure - :%+8ld\n", pressval);
    printf("Pressure Temperature:%+3d\n", press_sensor->value(TEMP));

    /*############################################################*/
    //Power Monitoring
    printf("V: %d, I: %d\n", batt_sensor->value(BATTERY_VOLTAGE),
        batt_sensor->value(BATTERY_CURRENT));
    //              tmp = adc_get_value_from(PWR_MONITOR_ICC_ADC);
    //             
    //              tmp = adc_get_value_from(PWR_MONITOR_VCC_ADC);
    //            

    /*############################################################*/
    //Magnetic Monitoring
    printf("Mag - x: %d, y: %d, z: %d\n",
          mag_sensor->value(MAG_X_RAW),
          mag_sensor->value(MAG_Y_RAW),
          mag_sensor->value(MAG_Z_RAW));


    /*############################################################*/
    /*############################################################*/
    //microSD Test
    //              uint8_t buffer[512];
    //              uint16_t j;
    //              microSD_init();
    //              for (j = 0; j < 512; j++) {
    //                      buffer[j] = 'u';
    //                      buffer[j + 1] = 'e';
    //                      buffer[j + 2] = 'r';
    //                      buffer[j + 3] = '\n';
    //
    //                      j = j + 3;
    //              }
    //
    //              microSD_write_block(2, buffer);
    //
    //              microSD_read_block(2, buffer);
    //
    //              for (j = 0; j < 512; j++) {
    //                      printf(buffer[j]);
    //              }
    //              microSD_deinit();
    //
    /*############################################################*/
    //Flash Test
    //    uint8_t buffer[512];
    //    uint16_t j, i;
    //
    //    for (i = 0; i < 10; i++) {
    //      //at45db_erase_page(i);
    //      for (j = 0; j < 512; j++) {
    //        buffer[j] = i;
    //      }
    //      at45db_write_buffer(0, buffer, 512);
    //
    //      at45db_buffer_to_page(i);
    //
    //      at45db_read_page_bypassed(i, 0, buffer, 512);
    //
    //      for (j = 0; j < 512; j++) {
    //        printf("%02x", buffer[j]);
    //        if (((j + 1) % 2) == 0)
    //          printf(" ");
    //        if (((j + 1) % 32) == 0)
    //          printf("\n");
    //      }
    //    }

  }


  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
