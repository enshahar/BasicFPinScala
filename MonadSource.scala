// 단순한 박스 타입
case class Boxed[T](value:T);

// 로그를 추가한 타입
case class Logged[T](value:T, log:List[String])

// 스칼라 옵션과 비슷한 타입
abstract class MyOption[+A]
case class MySome[+A](x: A) extends MyOption[A]
case object MyNone extends MyOption[Nothing] 

// 지연계산 정수
class Lazy[T](value: ()=>T) {
   def getValue():T = value()
}

// 리스트
abstract class MyList[+A] 
case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]
case object MyNil extends MyList[Nothing]

// 리스트 생성 테스트
val x1:MyList[Int] = Cons(1,Cons(2,Cons(8,MyNil)))


// Int => C[Int] 생성 함수들
// 케이스클래스인 경우 기본제공되는 클래스 짝 객체에 있는 생성자함수를 사용 가능하며,
// 아닌 경우 new를 사용하자.
def initBoxed(x:Int):Boxed[Int] = Boxed(x)
def initLogged(x:Int):Logged[Int] = Logged(x, List())
def initMyOption(x:Int):MyOption[Int] = MySome(x)
def initLazy(x: =>Int):Lazy[Int] = new Lazy(()=>x)
def initMyList(x:Int):MyList[Int] = Cons(x,MyNil)

// 값을 두배로 만드는 함수와 sqrt 취하는 함수의 정보추가 버전 만들기 예제

// 기본 정수=>정수 함수
def double(x:Int):Int = x + x
def sqrt(x:Int):Int = Math.sqrt(x).toInt

// 박스: 감싸기만 할 뿐 추가되는 정보 없음
def doubleBoxed(x:Int):Boxed[Int] = Boxed(x+x)
def sqrtBoxed(x:Int):Boxed[Int] = Boxed(Math.sqrt(x).toInt)


// 로깅: 관련 정보를 문자열 리스트로 기록 
def doubleLogged(x:Int):Logged[Int] = Logged(x+x, List("double("+x+") = " + (x+x)))
def sqrtLogged(x:Int):Logged[Int] = Logged(Math.sqrt(x).toInt, List("sqrt("+x+").toInt = " + Math.sqrt(x).toInt))

// 옵션: 값에 따라 오류값(MyNone)이나 정상값(MySome)이 가능
def doubleMyOption(x:Int):MyOption[Int] = MySome(x+x)
def sqrtMyOption(x:Int):MyOption[Int] = if(x>=0) MySome(Math.sqrt(x).toInt) else MyNone

// 지연계산: 인자를 계산하지 않기 위해 call by name을 사용하고, 함수를 만들어 계산을 지연시켜둠
def doubleLazy(x: =>Int):Lazy[Int] = new Lazy(()=>{print("lazy double(" + x + ") run\n");x+x})
def sqrtLazy(x: =>Int):Lazy[Int] = new Lazy(()=>{print("lazy sqrt(" + x + ") run\n");Math.sqrt(x).toInt})

// 리스트: 계산 결과를 리스트로 만듦
def doubleMyList(x:Int):MyList[Int] = Cons(x+x,MyNil)
def sqrtMyList(x:Int):MyList[Int] = Cons(Math.sqrt(x).toInt,MyNil)


// 두 함수를 조합하는 합성 함수. 커리되지 않은 버전
def o[T,V,U](f:T=>V, g:V=>U) = (x:T) => g(f(x))

// double을 먼저 하고 sqrt를 취하는 함수를 o를 사용해 만듦
val doubleThenSqrt = o(double, sqrt)

// doubleThenSqrt 테스트
// 결과: res0: Int = 4
doubleThenSqrt(8)

// 합성 함수의 커리 버전
def o2[T,V,U](f:T=>V) = (g:V=>U) => (x:T) => g(f(x))

// double을 먼저 하고 sqrt를 취하는 함수를 o2를 사용해 만들고 씀
// 결과: doubleThenSqrt2Result: Int = 4
val doubleThenSqrt2Result = o2(double)(sqrt)(8)

// 오류가 나는 예:
//scala> o(doubleBoxed,sqrtBoxed)
//<console>:13: error: type mismatch;
// found   : Int => Boxed[Int]
// required: Boxed[Int] => Boxed[Int]
//              o(doubleBoxed,sqrtBoxed)

// 박스합수를 함성하기 위한 박스화 함수
// "인자에서 내부 값을 꺼내서 함수를 적용하고 반환한다"라는 기본 패턴을 보라.
def mkBoxedFun(f:Int=>Boxed[Int]) = (x:Boxed[Int]) => {
	val value = x.value    // x(박싱된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}


// 합성테스트
// 결과: x2: Boxed[Int] = Boxed(4)
val x2 = o(mkBoxedFun(doubleBoxed), mkBoxedFun(sqrtBoxed))(initBoxed(8))

// 로그 : x의 내부 정보중 값만 노출시켜 함수 적용후 반환하는 잘못된 구현
def mkLoggedFun(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}

// 테스트: 결과를 보면 최초 함수를 적용했던 로그가 날아가버림
// 결과: x3: Logged[Int] = Logged(4,List(sqrt(16).toInt = 4))
val x3 = o(mkLoggedFun(doubleLogged), mkLoggedFun(sqrtLogged))(initLogged(8))

// 로그: 로그도 가져와서 서로 붙여줌
def mkLoggedFunRevised(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val log = x.log        // x에서 로그 가져오기
	val value2 = f(value)  // 함수 적용
	Logged(value2.value, log:::value2.log)
}

// 로그 적용 함수 합성 실험 
// x4: Logged[Int] = Logged(4,List(double(8) = 16, sqrt(16).toInt = 4))
val x4 = o(mkLoggedFunRevised(doubleLogged), mkLoggedFunRevised(sqrtLogged))(initLogged(8))


// 옵션: MyNone인 경우를 따져줘야 함
def mkMyOptionFun(f: (Int=>MyOption[Int])) = (x: MyOption[Int]) => x match {
  case MySome(x) => { // 값을 노출시키는 작업은 패턴매칭으로 됨
    val value2 = f(x) // f 적용하기
    value2 // 값 반납하기
  }
  case MyNone => MyNone	
}

// 옵션 합성 실험: 정상값인 경우
val x5 = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(8))
// 두번째 함수에서 오류가 나는 경우 
val errval = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(-8))
// 첫번째 함수에서 오류가 나는 경우
val errval2 = o(mkMyOptionFun(sqrtMyOption), mkMyOptionFun(doubleMyOption))(MySome(-8))

// 앞의 두 값의 실행 결과
//scala> val x5 = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(8))
//x5: MyOption[Int] = MySome(4)
//
//scala> val errval = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(-8))
//errval: MyOption[Int] = MyNone
//
//scala> val errval2 = o(mkMyOptionFun(sqrtMyOption), mkMyOptionFun(doubleMyOption))(MySome(-8))
//errval2: MyOption[Int] = MyNone

// 지연계산: 내부 정보를 노출시키는 부분도 함수로 만들어서 계산되지 않게 함.
def mkLazyFun(f: (Int=>Lazy[Int])) = (x: Lazy[Int]) => {
	def value = x.getValue // x서 내부의 값 노출시키기(def로 했으므로 계산하지 않음)
	def tmpFun() = {	     // x 내부 값을 계산하지 않게 함수를 하나 정의
		val y = f(value)
		y.getValue()
	}
	new Lazy(tmpFun)
}

// 테스트
val x6 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
x6.getValue()

//실행결과
//scala> val x6 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
//x6: Lazy[Int] = Lazy@2346d950
//
//scala> x6.getValue()
//lazy double(8) run
//lazy sqrt(16) run
//res15: Int = 4


// 리스트: 리스트내의 모든 원소에 함수를 적용한 다음, 이를 펼침
def mkMyListFun(f:Int=>MyList[Int]) = (x:MyList[Int]) => {
  // f가 만드는 리스트를 모두 합쳐야 하기 때문에 결과적으로는 
  // flatMap과 비슷한 일을 해야 한다.
  def append(l1:MyList[Int], l2:MyList[Int]):MyList[Int] = l1 match {
    case Cons(h,t) => {
      Cons(h,append(t,l2))
    }
    case MyNil => l2
  }
  def mapAll(l:MyList[Int]):MyList[Int] = l match {
    case Cons(h,t) => {
      val value2 = f(h)
      val remain = mapAll(t)
      append(value2,remain)
    }
    case MyNil => MyNil
  }
  mapAll(x)
}

// x1은 앞에서 만들었던 리스트이다.
// x1 = Cons(1,Cons(2,Cons(8,MyNil)))
val x7 = o(mkMyListFun(doubleMyList), mkMyListFun(sqrtMyList))(x1)

//실행결과:
//scala> val x7 = o(mkMyListFun(doubleMyList), mkMyListFun(sqrtMyList))(x1)
//x7: MyList[Int] = Cons(1,Cons(2,Cons(4,MyNil)))
