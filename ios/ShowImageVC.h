//
//  ShowImageVC.h
//  imageCropPicker
//
//  Created by 欧阳伟坚 on 2018/4/3.
//  Copyright © 2018年 Ivan Pusic. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ShowImageVC : UIViewController

@property(nonatomic, strong) UIImage *dataImage;

@property(nonatomic, strong) NSString *location;

@property(nonatomic, strong) NSString *name;

@property(nonatomic, strong) UIViewController *superVC;

@property(nonatomic)BOOL isAcross;
@end
