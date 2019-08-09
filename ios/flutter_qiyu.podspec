#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_qiyu'
  s.version          = '0.0.1'
  s.summary          = 'Neteast Qiyu customer service for flutter'
  s.description      = <<-DESC
Neteast Qiyu customer service for flutter
                       DESC
  s.homepage         = 'https://github.com/HoZanHoi/flutter_qiyu'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'kevinho0706@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.dependency 'QIYU_iOS_SDK', '~> 5.1.0'

  s.ios.deployment_target = '8.0'
end

